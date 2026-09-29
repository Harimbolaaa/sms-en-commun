import { Client, Databases, ID, Permission, Role } from 'node-appwrite';
import fs from 'fs';
import path from 'path';

// Lire les configurations depuis l'environnement ou .env
function loadEnv() {
  const envPath = path.resolve(process.cwd(), '.env');
  const env = { ...process.env };
  if (fs.existsSync(envPath)) {
    const lines = fs.readFileSync(envPath, 'utf-8').split('\n');
    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) continue;
      const idx = trimmed.indexOf('=');
      if (idx > 0) {
        const key = trimmed.substring(0, idx).trim();
        const value = trimmed.substring(idx + 1).trim();
        if (!env[key]) env[key] = value;
      }
    }
  }
  return env;
}

const env = loadEnv();

const ENDPOINT = env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = env.APPWRITE_PROJECT_ID || '6a22c323001a2537c74e';
const DATABASE_ID = env.APPWRITE_DATABASE_ID || '6aa39c2a0008d6d8e8e4';
const COLLECTION_ID = env.APPWRITE_COLLECTION_ID || 'sms_logs';
const API_KEY = env.APPWRITE_API_KEY || '';

console.log('====================================================');
console.log('🚀 INITIALISATION APPWRITE CLOUD BACKEND');
console.log('====================================================');
console.log(`📍 Endpoint    : ${ENDPOINT}`);
console.log(`📦 Projet ID   : ${PROJECT_ID}`);
console.log(`🗄️  Database ID : ${DATABASE_ID}`);
console.log(`📑 Collection  : ${COLLECTION_ID}`);
console.log(`🔑 API Key     : ${API_KEY ? API_KEY.substring(0, 16) + '...' : 'NON DÉFINIE'}`);
console.log('----------------------------------------------------');

if (!API_KEY) {
  console.error('❌ Erreur: Aucune clé API Appwrite trouvée dans les variables d\'environnement (.env)');
  process.exit(1);
}

const client = new Client()
  .setEndpoint(ENDPOINT)
  .setProject(PROJECT_ID)
  .setKey(API_KEY);

const databases = new Databases(client);

async function init() {
  try {
    // 1. Vérification de la Base de Données
    console.log(`\n🔍 1. Vérification de la base de données "${DATABASE_ID}"...`);
    let db;
    try {
      db = await databases.get(DATABASE_ID);
      console.log(`✅ Base de données trouvée: "${db.name}" (${db.$id})`);
    } catch (err) {
      console.log(`⚠️ Base non trouvée, tentative de création...`);
      db = await databases.create(DATABASE_ID, 'smsAppelCommun');
      console.log(`✅ Base de données créée: "${db.name}" (${db.$id})`);
    }

    // 2. Vérification / Création de la collection (table)
    console.log(`\n🔍 2. Vérification de la collection (table) "${COLLECTION_ID}"...`);
    let collection;
    try {
      collection = await databases.getCollection(DATABASE_ID, COLLECTION_ID);
      console.log(`✅ Collection existante: "${collection.name}" (${collection.$id})`);
    } catch (err) {
      console.log(`⚠️ Collection absente, création en cours...`);
      collection = await databases.createCollection(
        DATABASE_ID,
        COLLECTION_ID,
        COLLECTION_ID,
        [
          Permission.read(Role.any()),
          Permission.create(Role.any()),
          Permission.update(Role.any()),
          Permission.delete(Role.any())
        ],
        false // documentSecurity
      );
      console.log(`✅ Collection créée avec succès: "${collection.name}" (${collection.$id})`);
    }

    // 3. Configuration des attributs (colonnes de la table)
    console.log(`\n🛠️  3. Configuration des attributs de la table...`);
    const existingAttrs = collection.attributes ? collection.attributes.map(a => a.key) : [];

    const attributesToCreate = [
      { type: 'string', key: 'sender', size: 255, required: false },
      { type: 'string', key: 'rawMessage', size: 10000, required: false },
      { type: 'string', key: 'referenceId', size: 255, required: false },
      { type: 'string', key: 'operator', size: 255, required: false },
      { type: 'integer', key: 'parsedAmount', required: false },
      { type: 'integer', key: 'createdAtUnix', required: false }
    ];

    for (const attr of attributesToCreate) {
      if (existingAttrs.includes(attr.key)) {
        console.log(`   🔹 Attribut existant: "${attr.key}" (${attr.type})`);
      } else {
        console.log(`   ➕ Création de l'attribut: "${attr.key}" (${attr.type})...`);
        try {
          if (attr.type === 'string') {
            await databases.createStringAttribute(DATABASE_ID, COLLECTION_ID, attr.key, attr.size, attr.required);
          } else if (attr.type === 'integer') {
            await databases.createIntegerAttribute(DATABASE_ID, COLLECTION_ID, attr.key, attr.required);
          }
          console.log(`   ✅ Attribut "${attr.key}" ajouté avec succès.`);
        } catch (attrErr) {
          console.log(`   ℹ️ Note sur "${attr.key}": ${attrErr.message}`);
        }
      }
    }

    // 4. Test de synchronisation / insertion de document avec le SDK
    console.log(`\n🧪 4. Test de synchronisation de données avec le SDK Appwrite...`);
    const testDoc = await databases.createDocument(
      DATABASE_ID,
      COLLECTION_ID,
      ID.unique(),
      {
        sender: 'CLI-SDK-TEST',
        rawMessage: 'Paiement reçu de 25000 Ar ref: MVO123456 pour Harry',
        parsedAmount: 25000,
        referenceId: 'MVO123456',
        operator: 'MVOLA',
        createdAtUnix: Date.now()
      }
    );

    console.log(`✅ Document de test créé dans Appwrite Cloud !`);
    console.log(`   ID du document: ${testDoc.$id}`);
    console.log(`   Expéditeur: ${testDoc.sender}`);
    console.log(`   Montant analysé: ${testDoc.parsedAmount} Ar`);
    console.log(`   Opérateur: ${testDoc.operator}`);

    // 5. Lecture des documents pour valider la synchronisation bidirectionnelle
    console.log(`\n📋 5. Vérification de la liste des documents synchronisés...`);
    const list = await databases.listDocuments(DATABASE_ID, COLLECTION_ID);
    console.log(`✅ Total de documents synchronisés dans la table: ${list.total}`);
    list.documents.slice(0, 3).forEach((d, i) => {
      console.log(`   [${i + 1}] ID: ${d.$id} | ${d.sender} -> ${d.parsedAmount} Ar (${d.operator})`);
    });

    console.log('\n====================================================');
    console.log('🎉 TOUS LES TESTS SONT RÉUSSIS ! SYNCHRONISATION PRÊTE.');
    console.log('====================================================');

  } catch (error) {
    console.error('\n❌ Échec de l\'initialisation Appwrite:', error);
    process.exit(1);
  }
}

init();

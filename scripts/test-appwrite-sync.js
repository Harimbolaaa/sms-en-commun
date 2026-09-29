import { Client, Databases, ID } from 'node-appwrite';
import fs from 'fs';
import path from 'path';

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

const client = new Client()
  .setEndpoint(ENDPOINT)
  .setProject(PROJECT_ID)
  .setKey(API_KEY);

const databases = new Databases(client);

async function runSyncTest() {
  console.log('🔄 Envoi d\'une transaction de test vers Appwrite Cloud...');
  const refNum = Math.floor(10000000 + Math.random() * 90000000);
  const amount = (Math.floor(Math.random() * 50) + 1) * 5000;
  
  try {
    const doc = await databases.createDocument(
      DATABASE_ID,
      COLLECTION_ID,
      ID.unique(),
      {
        sender: 'YAS',
        rawMessage: `Vous avez recu ${amount} Ar de HARRY (+261340000000). Ref: ${refNum}. Nouveau solde: ${amount + 12500} Ar.`,
        parsedAmount: amount,
        referenceId: `${refNum}`,
        operator: 'MVOLA',
        createdAtUnix: Date.now()
      }
    );

    console.log('✅ SYNCHRONISATION RÉUSSIE !');
    console.log(JSON.stringify(doc, null, 2));
  } catch (err) {
    console.error('❌ Erreur de synchronisation:', err);
    process.exit(1);
  }
}

runSyncTest();

import { Client, Users, ID } from 'node-appwrite';
import fs from 'fs';
import path from 'path';

function loadEnv() {
  const envPath = path.resolve(process.cwd(), '.env');
  const env = { ...process.env };
  if (fs.existsSync(envPath)) {
    fs.readFileSync(envPath, 'utf-8').split('\n').forEach(line => {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) return;
      const idx = trimmed.indexOf('=');
      if (idx > 0) env[trimmed.substring(0, idx).trim()] = trimmed.substring(idx + 1).trim();
    });
  }
  return env;
}

const env = loadEnv();
const endpoint = env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const projectId = env.APPWRITE_PROJECT_ID || '6a22c323001a2537c74e';
const apiKey = env.APPWRITE_API_KEY;

if (!apiKey) {
  console.error('❌ Erreur: APPWRITE_API_KEY non trouvée dans .env');
  process.exit(1);
}

const client = new Client()
  .setEndpoint(endpoint)
  .setProject(projectId)
  .setKey(apiKey);

const users = new Users(client);

// Helper pour convertir un numéro de téléphone en faux email Appwrite
export function phoneToFakeEmail(phone) {
  const cleanPhone = phone.replace(/[^0-9]/g, '');
  return `${cleanPhone}@appwrite.local`;
}

async function run() {
  const args = process.argv.slice(2);
  const rawPhone = args[0] || '0444444444';
  const password = args[1] || 'password';
  const role = args[2] || 'admin';
  const name = args[3] || (role === 'admin' ? `Admin (${rawPhone})` : `Client (${rawPhone})`);

  const email = phoneToFakeEmail(rawPhone);

  console.log('====================================================');
  console.log('👤 CRÉATION / CONFIGURATION UTILISATEUR APPWRITE');
  console.log('====================================================');
  console.log(`📱 Numéro Téléphone : ${rawPhone}`);
  console.log(`📧 Fake Email généré: ${email}`);
  console.log(`🔑 Rôle             : ${role}`);
  console.log('----------------------------------------------------');

  try {
    const list = await users.list();
    const existing = list.users.find(u => u.email === email);

    let userId;
    if (existing) {
      userId = existing.$id;
      console.log(`ℹ️  L'utilisateur existe déjà (ID: ${userId}). Mise à jour du mot de passe et rôle...`);
      await users.updatePassword(userId, password);
      await users.updateLabels(userId, [role]);
      console.log('✅ Mot de passe et labels mis à jour avec succès.');
    } else {
      const newUser = await users.create(
        ID.unique(),
        email,
        undefined,
        password,
        name
      );
      userId = newUser.$id;
      await users.updateLabels(userId, [role]);
      console.log(`✅ Utilisateur créé avec succès (ID: ${userId}) !`);
    }

    console.log('----------------------------------------------------');
    console.log(`🎉 Compte prêt : Login avec ${rawPhone} / ${password}`);
    console.log('====================================================');
  } catch (error) {
    console.error('❌ Erreur lors de la gestion utilisateur:', error.message || error);
    process.exit(1);
  }
}

run();

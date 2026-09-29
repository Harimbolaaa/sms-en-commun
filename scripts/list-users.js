import { Client, Users } from 'node-appwrite';
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

const client = new Client()
  .setEndpoint(endpoint)
  .setProject(projectId)
  .setKey(apiKey);

const users = new Users(client);

async function run() {
  try {
    const list = await users.list();
    console.log('====================================================');
    console.log(`📋 LISTE DES UTILISATEURS APPWRITE CLOUD (${list.total})`);
    console.log('====================================================');
    list.users.forEach((u, index) => {
      const phoneDisplay = u.email.includes('@appwrite.local') ? u.email.replace('@appwrite.local', '') : u.phone || u.email;
      console.log(`[${index + 1}] ID: ${u.$id}`);
      console.log(`    Nom     : ${u.name}`);
      console.log(`    Tél/Login: ${phoneDisplay} (Email: ${u.email})`);
      console.log(`    Labels  : [${(u.labels || []).join(', ')}]`);
      console.log(`    Statut  : ${u.status ? 'Actif' : 'Désactivé'}`);
      console.log('----------------------------------------------------');
    });
  } catch (error) {
    console.error('❌ Erreur récupération utilisateurs:', error.message || error);
  }
}

run();

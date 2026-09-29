const ENDPOINT = 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = '6a22c323001a2537c74e';
const DATABASE_ID = '6aa39c2a0008d6d8e8e4';
const COLLECTION_ID = 'sms_logs';
const API_KEY = 'standard_45db327f59e4d9c86616d4eb92bde54e04bc5175e909d5ac318909357a2e6c26cc72686fa02c562d358c723b37199a29f6e47363e7a139ea5eac2aa4f995f1c8d60fef613c89cdcd7b2d491d966d4d4f0fad1238a2b8c5eb5ba85306f7460fe60d3e9b03a9609a354dff899f495676b06fe31860b3f56731a4c30dee9bdd20aa';

const headers = {
  'X-Appwrite-Project': PROJECT_ID,
  'X-Appwrite-Key': API_KEY,
  'Content-Type': 'application/json'
};

async function run() {
  const attrs = [
    { key: 'clientPhone', size: 64 },
    { key: 'clientUserId', size: 128 }
  ];

  for (const attr of attrs) {
    console.log(`Adding ${attr.key} to sms_logs...`);
    const res = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/attributes/string`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ key: attr.key, size: attr.size, required: false })
    });
    console.log(`Result for ${attr.key}:`, res.status);
  }
}

run();

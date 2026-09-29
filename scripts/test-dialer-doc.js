const ENDPOINT = 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = '6a22c323001a2537c74e';
const DATABASE_ID = '6aa39c2a0008d6d8e8e4';
const COLLECTION_ID = 'user_dialer_codes';
const API_KEY = 'standard_45db327f59e4d9c86616d4eb92bde54e04bc5175e909d5ac318909357a2e6c26cc72686fa02c562d358c723b37199a29f6e47363e7a139ea5eac2aa4f995f1c8d60fef613c89cdcd7b2d491d966d4d4f0fad1238a2b8c5eb5ba85306f7460fe60d3e9b03a9609a354dff899f495676b06fe31860b3f56731a4c30dee9bdd20aa';

const headers = {
  'X-Appwrite-Project': PROJECT_ID,
  'X-Appwrite-Key': API_KEY,
  'Content-Type': 'application/json'
};

async function test() {
  const docId = '6aa3a94c01e1f150b766'; // user2 id
  const body = {
    documentId: docId,
    data: {
      userId: docId,
      userPhone: '0340000002',
      secretCode: '4444',
      isCustomized: false,
      updatedAt: Date.now()
    }
  };

  let res = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/documents`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body)
  });

  if (res.status === 409) {
    console.log('Document already exists, updating...');
    res = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/documents/${docId}`, {
      method: 'PATCH',
      headers,
      body: JSON.stringify({ data: body.data })
    });
  }

  const result = await res.json();
  console.log('Result:', result);
}

test();

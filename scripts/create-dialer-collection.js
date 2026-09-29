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

async function run() {
  try {
    console.log(`Checking collection ${COLLECTION_ID}...`);
    let res = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}`, {
      method: 'GET',
      headers
    });

    if (res.status === 404) {
      console.log(`Collection ${COLLECTION_ID} not found. Creating...`);
      res = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          collectionId: COLLECTION_ID,
          name: 'user_dialer_codes',
          permissions: [
            'read("any")',
            'create("any")',
            'update("any")',
            'delete("any")'
          ],
          documentSecurity: false
        })
      });
      const data = await res.json();
      console.log('Collection created:', data);
    } else {
      console.log(`Collection already exists.`);
    }

    // Get current attributes
    res = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}`, {
      method: 'GET',
      headers
    });
    const colData = await res.json();
    const existingKeys = (colData.attributes || []).map(a => a.key);

    const attrs = [
      { key: 'userPhone', type: 'string', size: 64, required: false },
      { key: 'secretCode', type: 'string', size: 16, required: false },
      { key: 'isCustomized', type: 'boolean', required: false },
      { key: 'userId', type: 'string', size: 128, required: false },
      { key: 'updatedAt', type: 'integer', required: false }
    ];

    for (const attr of attrs) {
      if (!existingKeys.includes(attr.key)) {
        console.log(`Creating attribute ${attr.key}...`);
        let attrRes;
        if (attr.type === 'string') {
          attrRes = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/attributes/string`, {
            method: 'POST',
            headers,
            body: JSON.stringify({ key: attr.key, size: attr.size, required: attr.required })
          });
        } else if (attr.type === 'boolean') {
          attrRes = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/attributes/boolean`, {
            method: 'POST',
            headers,
            body: JSON.stringify({ key: attr.key, required: attr.required })
          });
        } else if (attr.type === 'integer') {
          attrRes = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/attributes/integer`, {
            method: 'POST',
            headers,
            body: JSON.stringify({ key: attr.key, required: attr.required })
          });
        }
        console.log(`Result for ${attr.key}:`, attrRes.status);
      } else {
        console.log(`Attribute ${attr.key} already exists.`);
      }
    }

    console.log('\nSUCCESS! The collection user_dialer_codes is ready in Appwrite Database!');
  } catch (e) {
    console.error('Error:', e);
  }
}

run();

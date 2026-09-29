const ENDPOINT = 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = '6a22c323001a2537c74e';
const DATABASE_ID = '6aa39c2a0008d6d8e8e4';
const API_KEY = 'standard_45db327f59e4d9c86616d4eb92bde54e04bc5175e909d5ac318909357a2e6c26cc72686fa02c562d358c723b37199a29f6e47363e7a139ea5eac2aa4f995f1c8d60fef613c89cdcd7b2d491d966d4d4f0fad1238a2b8c5eb5ba85306f7460fe60d3e9b03a9609a354dff899f495676b06fe31860b3f56731a4c30dee9bdd20aa';

const headers = {
  'X-Appwrite-Project': PROJECT_ID,
  'X-Appwrite-Key': API_KEY,
  'Content-Type': 'application/json'
};

async function createConfigCollection() {
  const url = `${ENDPOINT}/databases/${DATABASE_ID}/collections`;
  const res = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      collectionId: 'app_config',
      name: 'app_config',
      permissions: ['read("any")', 'create("any")', 'update("any")', 'delete("any")']
    })
  });
  const data = await res.json();
  console.log('Create collection result:', res.status, data.$id || data.message);

  if (res.status === 201 || (data.code === 409)) {
    // Add attributes
    const attrUrl = `${ENDPOINT}/databases/${DATABASE_ID}/collections/app_config/attributes/string`;
    const a1 = await fetch(attrUrl, {
      method: 'POST',
      headers,
      body: JSON.stringify({ key: 'configKey', size: 255, required: true })
    });
    console.log('attr configKey:', a1.status);

    const a2 = await fetch(attrUrl, {
      method: 'POST',
      headers,
      body: JSON.stringify({ key: 'configValue', size: 10000, required: true })
    });
    console.log('attr configValue:', a2.status);

    const a3 = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/app_config/attributes/integer`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ key: 'updatedAt', required: false })
    });
    console.log('attr updatedAt:', a3.status);

    const a4 = await fetch(attrUrl, {
      method: 'POST',
      headers,
      body: JSON.stringify({ key: 'updatedBy', size: 255, required: false })
    });
    console.log('attr updatedBy:', a4.status);
  }
}

createConfigCollection();

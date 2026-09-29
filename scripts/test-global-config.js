const ENDPOINT = 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = '6a22c323001a2537c74e';
const DATABASE_ID = '6aa39c2a0008d6d8e8e4';
const COLLECTION_ID = 'app_config';
const API_KEY = 'standard_45db327f59e4d9c86616d4eb92bde54e04bc5175e909d5ac318909357a2e6c26cc72686fa02c562d358c723b37199a29f6e47363e7a139ea5eac2aa4f995f1c8d60fef613c89cdcd7b2d491d966d4d4f0fad1238a2b8c5eb5ba85306f7460fe60d3e9b03a9609a354dff899f495676b06fe31860b3f56731a4c30dee9bdd20aa';

const headers = {
  'X-Appwrite-Project': PROJECT_ID,
  'X-Appwrite-Key': API_KEY,
  'Content-Type': 'application/json'
};

async function testGlobalConfigDoc() {
  // wait 2 seconds for attributes to be ready
  await new Promise(r => setTimeout(r, 2000));

  const targetUrl = `${ENDPOINT}/databases/${DATABASE_ID}/collections/sms_logs/documents`;
  const defaultHeaders = [
    {
      id: "default_appwrite_project",
      name: "X-Appwrite-Project",
      value: PROJECT_ID,
      isEnabled: true,
      description: "Identifiant de projet Appwrite"
    },
    {
      id: "default_appwrite_key",
      name: "X-Appwrite-Key",
      value: API_KEY,
      isEnabled: true,
      description: "Clé secrète API Appwrite Cloud"
    },
    {
      id: "default_content_type",
      name: "Content-Type",
      value: "application/json",
      isEnabled: true,
      description: "Format JSON Appwrite"
    }
  ];

  const configData = {
    targetUrl: targetUrl,
    customHeadersJson: JSON.stringify(defaultHeaders),
    allowedSenders: "YAS, TELMA, ORANGE, AIRTEL, MVOLA",
    isSenderFilterEnabled: false,
    isRegexEnabled: true,
    sendOnlyExtracted: false,
    isAutoForwardEnabled: true
  };

  const docUrl = `${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/documents`;
  
  // Try create document with ID 'global_settings'
  const res = await fetch(docUrl, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      documentId: 'global_settings',
      data: {
        configKey: 'global_settings',
        configValue: JSON.stringify(configData),
        updatedAt: Date.now(),
        updatedBy: 'admin'
      }
    })
  });

  const resData = await res.json();
  console.log('Create doc status:', res.status, resData.$id || resData.message);

  // If already exists, update
  if (res.status === 409) {
    const updateUrl = `${docUrl}/global_settings`;
    const updRes = await fetch(updateUrl, {
      method: 'PATCH',
      headers,
      body: JSON.stringify({
        data: {
          configKey: 'global_settings',
          configValue: JSON.stringify(configData),
          updatedAt: Date.now(),
          updatedBy: 'admin'
        }
      })
    });
    console.log('Update doc status:', updRes.status);
  }
}

testGlobalConfigDoc();

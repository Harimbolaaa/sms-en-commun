const ENDPOINT = 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = '6a22c323001a2537c74e';
const DATABASE_ID = '6aa39c2a0008d6d8e8e4';
const COLLECTION_ID = 'sms_logs';
const API_KEY = 'standard_45db327f59e4d9c86616d4eb92bde54e04bc5175e909d5ac318909357a2e6c26cc72686fa02c562d358c723b37199a29f6e47363e7a139ea5eac2aa4f995f1c8d60fef613c89cdcd7b2d491d966d4d4f0fad1238a2b8c5eb5ba85306f7460fe60d3e9b03a9609a354dff899f495676b06fe31860b3f56731a4c30dee9bdd20aa';

const targetUrl = `${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/documents`;

async function testSendSms() {
  const payload = {
    documentId: 'unique()',
    data: {
      sender: "YAS",
      rawMessage: "19/08/26 08:16 Credit YAS recu : 500 Ar de Mioranirina 0386135942. Solde YAS actuel : 504 Ar. Ref: 5661921553",
      parsedAmount: 500,
      referenceId: "5661921553",
      operator: "MVOLA",
      createdAtUnix: Date.now(),
      clientPhone: "0340000002",
      clientUserId: "user_0340000002"
    }
  };

  const headers = {
    'Content-Type': 'application/json',
    'X-Appwrite-Project': PROJECT_ID,
    'X-Appwrite-Key': API_KEY
  };

  const res = await fetch(targetUrl, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload)
  });

  const data = await res.json();
  console.log('Test send SMS status:', res.status, data.$id ? `SUCCESS (Doc ID: ${data.$id})` : data);
}

testSendSms();

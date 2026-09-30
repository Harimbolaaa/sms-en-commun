const ENDPOINT = 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = '6a22c323001a2537c74e';
const DATABASE_ID = '6aa39c2a0008d6d8e8e4';
const COLLECTION_ID = 'sms_logs';
const API_KEY = 'standard_45db327f59e4d9c86616d4eb92bde54e04bc5175e909d5ac318909357a2e6c26cc72686fa02c562d358c723b37199a29f6e47363e7a139ea5eac2aa4f995f1c8d60fef613c89cdcd7b2d491d966d4d4f0fad1238a2b8c5eb5ba85306f7460fe60d3e9b03a9609a354dff899f495676b06fe31860b3f56731a4c30dee9bdd20aa';

const targetUrl = `${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/documents`;

// Test the 10 simulation messages
async function runTest10() {
  console.log("=== TEST ENVOI 10 MESSAGES SIMULATION FILE D'ATTENTE ===");
  const carriers = [
    { op: "MVOLA", sender: "MVOLA", amount: 5000 },
    { op: "AIRTEL", sender: "AirtelMoney", amount: 10000 },
    { op: "ORANGE", sender: "OrangeMoney", amount: 2000 },
    { op: "TELMA", sender: "TELMA", amount: 1500 },
    { op: "YAS", sender: "YAS", amount: 500 },
    { op: "MVOLA", sender: "MVOLA", amount: 25000 },
    { op: "AIRTEL", sender: "AirtelMoney", amount: 3000 },
    { op: "ORANGE", sender: "OrangeMoney", amount: 50000 },
    { op: "TELMA", sender: "TELMA", amount: 1000 },
    { op: "YAS", sender: "YAS", amount: 8000 }
  ];

  let successCount = 0;
  for (let i = 0; i < carriers.length; i++) {
    const c = carriers[i];
    const ref = `REF-SIMUL-${Date.now() % 100000 + i}`;
    const payload = {
      documentId: 'unique()',
      data: {
        sender: c.sender,
        rawMessage: `Credit ${c.sender} recu : ${c.amount} Ar. Test file d'attente #${i + 1}. Ref: ${ref}`,
        parsedAmount: c.amount, // INTEGER
        referenceId: ref,
        operator: c.op,
        createdAtUnix: Date.now() + i * 500,
        clientPhone: "0340000001",
        clientUserId: "admin_test"
      }
    };

    const res = await fetch(targetUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Appwrite-Project': PROJECT_ID,
        'X-Appwrite-Key': API_KEY
      },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.status === 201) {
      successCount++;
      console.log(`Test #${i + 1} (${c.sender}, ${c.amount} Ar) -> SUCCESS 201 (Doc: ${data.$id})`);
    } else {
      console.error(`Test #${i + 1} FAILED (${res.status}):`, data.message);
    }
  }

  console.log(`\nRESULTAT FINAL : ${successCount}/10 REUSSIS !`);
}

runTest10();

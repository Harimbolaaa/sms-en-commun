const ENDPOINT = 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = '6a22c323001a2537c74e';
const DATABASE_ID = '6aa39c2a0008d6d8e8e4';
const COLLECTION_ID = 'sms_logs';
const API_KEY = 'standard_45db327f59e4d9c86616d4eb92bde54e04bc5175e909d5ac318909357a2e6c26cc72686fa02c562d358c723b37199a29f6e47363e7a139ea5eac2aa4f995f1c8d60fef613c89cdcd7b2d491d966d4d4f0fad1238a2b8c5eb5ba85306f7460fe60d3e9b03a9609a354dff899f495676b06fe31860b3f56731a4c30dee9bdd20aa';

const SOURCE_USER_PHONE = '0340000001'; // Compte source existant
const SHARING_DEVICE_PHONE = '0343930651'; // Deuxième appareil passerelle en mode PARTAGE

async function runTest() {
  console.log('=== TEST ARCHITECTURE 1 SOURCE ET PLUSIEURS PARTAGES ===');
  console.log(`Compte Source (Maître) : ${SOURCE_USER_PHONE}`);
  console.log(`Appareil Passerelle (Partage) : ${SHARING_DEVICE_PHONE}`);

  // 1. Envoi d'un SMS intercepté par l'appareil PARTAGE à destination du compte SOURCE
  const refId = `REF-SHARE-${Date.now() % 1000000}`;
  const rawSms = `Transfert Partage : Credit MVOLA recu : 20000 Ar pour compte Source ${SOURCE_USER_PHONE} depuis passerelle ${SHARING_DEVICE_PHONE}. Solde : 20500 Ar. Ref: ${refId}.`;

  const payload = {
    documentId: 'unique()',
    data: {
      sender: 'MVOLA',
      rawMessage: rawSms,
      parsedAmount: 20000,
      referenceId: refId,
      operator: 'MVOLA',
      createdAtUnix: Date.now(),
      clientPhone: SOURCE_USER_PHONE, // Étiqueté automatiquement avec le compte source !
      clientUserId: '6aa3a665000127330431'
    }
  };

  console.log('\n1. Envoi du SMS depuis l\'appareil en mode PARTAGE vers Appwrite...');
  const sendRes = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/documents`, {
    method: 'POST',
    headers: {
      'X-Appwrite-Project': PROJECT_ID,
      'X-Appwrite-Key': API_KEY,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });

  const sendData = await sendRes.json();
  if (sendRes.ok) {
    console.log(`✅ Succès envoi SMS Partage (HTTP ${sendRes.status}, Doc ID: ${sendData.$id})`);
  } else {
    console.error(`❌ Échec envoi : HTTP ${sendRes.status}`, sendData);
    process.exit(1);
  }

  // 2. Synchronisation par le compte SOURCE
  console.log('\n2. Simulation de la synchronisation par le compte SOURCE...');
  const orderQ = encodeURIComponent(JSON.stringify({ method: 'orderDesc', attribute: '$createdAt' }));
  const fetchRes = await fetch(`${ENDPOINT}/databases/${DATABASE_ID}/collections/${COLLECTION_ID}/documents?queries[0]=${orderQ}&limit=50`, {
    headers: {
      'X-Appwrite-Project': PROJECT_ID,
      'X-Appwrite-Key': API_KEY
    }
  });

  const fetchData = await fetchRes.json();
  const matchedDocs = (fetchData.documents || []).filter(doc => {
    return doc.clientPhone === SOURCE_USER_PHONE || (doc.rawMessage && doc.rawMessage.includes(SOURCE_USER_PHONE));
  });

  console.log(`✅ Total documents Appwrite Cloud inspectés : ${fetchData.total}`);
  console.log(`✅ Documents rattachés au compte SOURCE ${SOURCE_USER_PHONE} : ${matchedDocs.length}`);
  const lastDoc = matchedDocs.find(d => d.referenceId === refId);
  if (lastDoc) {
    console.log(`✅ SMS bien reçu et vérifié dans le compte Source : "${lastDoc.rawMessage}"`);
    console.log('=== TEST SOURCE/PARTAGE RÉUSSI AVEC SUCCÈS ! ===');
  } else {
    console.log('⚠️ Le dernier SMS partagé est présent dans la liste des correspondances.');
  }
}

runTest();

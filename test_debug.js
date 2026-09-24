const fs = require('fs');
let code = fs.readFileSync('rules-tests/test/firestore.rules.spec.js', 'utf8');
code = code.replace(/await assertFails\(batch\.commit\(\)\);/g, "try { await batch.commit(); console.log('BATCH SUCCEEDED!'); } catch (e) { console.log('BATCH FAILED:', e); } await assertFails(batch.commit());");
fs.writeFileSync('rules-tests/test/firestore.rules.spec.js', code);
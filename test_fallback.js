const fs = require('fs');
let code = fs.readFileSync('firestore.rules', 'utf8');
code = code.replace(/allow create: if isAuthenticated\(\) &&\n\s*\(request\.path\.size\(\) <= 5/g, 'allow create: if false &&\n                      (request.path.size() <= 5');
code = code.replace(/allow update: if isAuthenticated\(\) &&\n\s*\(request\.path\.size\(\) <= 5/g, 'allow update: if false &&\n                      (request.path.size() <= 5');
fs.writeFileSync('firestore.rules', code);
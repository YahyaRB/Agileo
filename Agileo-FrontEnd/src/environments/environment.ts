export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api/',
  keycloak: {
    url: 'http://localhost:8080',
    realm: 'agileo-realm',
    clientId: 'agileo-front-app'
  },
  fileUpload: {
    maxSize: 10485760, // 10MB
    allowedTypes: ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'jpg', 'jpeg', 'png', 'gif', 'txt']
  }
};

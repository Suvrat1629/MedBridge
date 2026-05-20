import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: process.env.NEXT_PUBLIC_KEYCLOAK_URL || 'http://localhost:8083',
  realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM || 'medbridge',
  clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'medbridge-web',
});

export default keycloak;

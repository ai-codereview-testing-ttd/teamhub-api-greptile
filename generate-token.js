#!/usr/bin/env node
// Generate a valid JWT token for TeamHub development
// Run with: node generate-token.js

const crypto = require('crypto');

const JWT_SECRET = 'teamhub-dev-jwt-secret-key-change-in-production-min-256-bits-long';
const JWT_ISSUER = 'teamhub-api';

function base64UrlEncode(str) {
  return Buffer.from(str)
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

function generateToken(userId, email, organizationId) {
  const header = {
    alg: 'HS256',
    typ: 'JWT'
  };

  const now = Math.floor(Date.now() / 1000);
  const expiry = now + (365 * 24 * 60 * 60); // 1 year

  const payload = {
    sub: userId,
    iss: JWT_ISSUER,
    email: email,
    organizationId: organizationId,
    iat: now,
    exp: expiry
  };

  const headerEncoded = base64UrlEncode(JSON.stringify(header));
  const payloadEncoded = base64UrlEncode(JSON.stringify(payload));
  const toSign = `${headerEncoded}.${payloadEncoded}`;

  const signature = crypto
    .createHmac('sha256', JWT_SECRET)
    .update(toSign)
    .digest('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');

  return `${toSign}.${signature}`;
}

// Generate token for John Doe (OWNER) from seed data
const token = generateToken(
  'user_01HQ3XK123',
  'john@acme.com',
  'org_01HQ3XJMR5E0987654321'
);

console.log('\n=== TeamHub JWT Token ===\n');
console.log('User: John Doe (OWNER)');
console.log('Email: john@acme.com');
console.log('Organization: Acme Corporation\n');
console.log('Token:');
console.log(token);
console.log('\n');

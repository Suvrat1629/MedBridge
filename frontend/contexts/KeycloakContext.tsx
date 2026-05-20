'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type Keycloak from 'keycloak-js';
import keycloakInstance from '@/lib/keycloak';

interface UserInfo {
  id?: string;
  username?: string;
  email?: string;
  name?: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
}

interface KeycloakContextType {
  keycloak: Keycloak;
  initialized: boolean;
  authenticated: boolean;
  token: string | undefined;
  userInfo: UserInfo;
  login: () => void;
  logout: () => void;
  getAuthHeaders: () => Record<string, string>;
}

const KeycloakContext = createContext<KeycloakContextType | null>(null);

export function KeycloakProvider({ children }: { children: React.ReactNode }) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [token, setToken] = useState<string | undefined>(undefined);

  useEffect(() => {
    keycloakInstance
      .init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      })
      .then((auth) => {
        setAuthenticated(auth);
        setToken(keycloakInstance.token);
        setInitialized(true);
      })
      .catch(() => {
        setInitialized(true);
      });

    keycloakInstance.onTokenExpired = () => {
      keycloakInstance
        .updateToken(60)
        .then(() => setToken(keycloakInstance.token))
        .catch(() => setAuthenticated(false));
    };

    keycloakInstance.onAuthRefreshSuccess = () => {
      setToken(keycloakInstance.token);
    };
  }, []);

  const userInfo: UserInfo =
    initialized && authenticated
      ? {
          id: keycloakInstance.tokenParsed?.sub,
          username: keycloakInstance.tokenParsed?.preferred_username,
          email: keycloakInstance.tokenParsed?.email,
          name: keycloakInstance.tokenParsed?.name,
          firstName: keycloakInstance.tokenParsed?.given_name,
          lastName: keycloakInstance.tokenParsed?.family_name,
          roles: keycloakInstance.tokenParsed?.realm_access?.roles ?? [],
        }
      : {};

  const login = useCallback(() => {
    keycloakInstance.login();
  }, []);

  const logout = useCallback(() => {
    keycloakInstance.logout({ redirectUri: window.location.origin });
  }, []);

  const getAuthHeaders = useCallback((): Record<string, string> => {
    if (keycloakInstance.token) {
      return { Authorization: `Bearer ${keycloakInstance.token}` };
    }
    return {};
  }, []);

  return (
    <KeycloakContext.Provider
      value={{ keycloak: keycloakInstance, initialized, authenticated, token, userInfo, login, logout, getAuthHeaders }}
    >
      {children}
    </KeycloakContext.Provider>
  );
}

export function useKeycloak() {
  const ctx = useContext(KeycloakContext);
  if (!ctx) throw new Error('useKeycloak must be used within KeycloakProvider');
  return ctx;
}

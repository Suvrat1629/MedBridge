'use client';

import { useEffect } from 'react';
import { useKeycloak } from '@/contexts/KeycloakContext';

export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { initialized, authenticated, login } = useKeycloak();

  useEffect(() => {
    if (initialized && !authenticated) {
      login();
    }
  }, [initialized, authenticated, login]);

  if (!initialized) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!authenticated) {
    return null;
  }

  return <>{children}</>;
}

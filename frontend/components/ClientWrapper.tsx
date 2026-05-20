'use client';

import { useState } from 'react';
import LoadingScreen from '@/components/LoadingScreen';
import GlobalBackground from '@/components/GlobalBackground';
import { KeycloakProvider } from '@/contexts/KeycloakContext';

interface ClientWrapperProps {
  children: React.ReactNode;
}

export default function ClientWrapper({ children }: ClientWrapperProps) {
  const [isLoading, setIsLoading] = useState(true);
  const [showBackground, setShowBackground] = useState(false);

  const handleLoadingComplete = () => {
    setIsLoading(false);
    setTimeout(() => {
      setShowBackground(true);
    }, 100);
  };

  return (
    <KeycloakProvider>
      {isLoading && (
        <LoadingScreen onLoadingComplete={handleLoadingComplete} />
      )}

      {!isLoading && (
        <>
          <GlobalBackground />
          <div
            className={`transition-opacity duration-300 ${
              showBackground ? 'opacity-100' : 'opacity-0'
            }`}
          >
            {children}
          </div>
        </>
      )}
    </KeycloakProvider>
  );
}

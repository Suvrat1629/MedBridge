'use client';

import { useState, useEffect, useCallback } from 'react';
import { useKeycloak } from '@/contexts/KeycloakContext';

export interface UserProfileData {
  id: string;
  email: string;
  displayName: string;
  phone: string;
  onboarded: boolean;
  createdAt: string;
}

const API_BASE = process.env.NEXT_PUBLIC_URL || 'http://localhost:8080';

export function useUserProfile() {
  const { authenticated, initialized, getAuthHeaders } = useKeycloak();
  const [profile, setProfile] = useState<UserProfileData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchProfile = useCallback(async () => {
    if (!authenticated) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/users/me`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Failed to load profile');
      setProfile(await res.json());
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [authenticated, getAuthHeaders]);

  const updateProfile = useCallback(async (data: Partial<Pick<UserProfileData, 'displayName' | 'phone' | 'onboarded'>>) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/users/me`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(data),
      });
      if (!res.ok) throw new Error('Failed to update profile');
      const updated = await res.json();
      setProfile(updated);
      return updated as UserProfileData;
    } catch (e: any) {
      setError(e.message);
      throw e;
    } finally {
      setLoading(false);
    }
  }, [getAuthHeaders]);

  useEffect(() => {
    if (initialized && authenticated) fetchProfile();
  }, [initialized, authenticated, fetchProfile]);

  return { profile, loading, error, fetchProfile, updateProfile };
}

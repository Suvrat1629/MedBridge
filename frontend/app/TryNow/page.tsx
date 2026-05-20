// app/TryNow/page.tsx
"use client";

import ProtectedRoute from '@/components/ProtectedRoute';
import { LiveDemoAutoplay } from './components';

export default function TryNowPage() {
  return (
    <ProtectedRoute>
      <div className="min-h-screen">
        <LiveDemoAutoplay />
      </div>
    </ProtectedRoute>
  );
}

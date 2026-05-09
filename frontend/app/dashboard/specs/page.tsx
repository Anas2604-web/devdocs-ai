'use client'
import { Upload, FileText, Plus } from 'lucide-react'

export default function SpecsPage() {
  return (
    <div style={{ padding: '40px 40px', maxWidth: 900 }}>
      <div className="animate-fade-up" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 32 }}>
        <div>
          <h1 style={{ fontSize: 22, fontWeight: 700, color: 'var(--ink)', letterSpacing: '-0.025em', marginBottom: 4 }}>API Specs</h1>
          <p style={{ fontSize: 13, color: 'var(--ink-4)' }}>Manage your OpenAPI specifications.</p>
        </div>
        <button className="btn-primary" style={{ fontSize: 13, padding: '8px 16px' }}>
          <Plus size={14} /> Upload spec
        </button>
      </div>

      {/* Empty state */}
      <div className="card animate-fade-up delay-50" style={{ padding: '72px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <div style={{ width: 52, height: 52, background: 'var(--paper-2)', border: '1.5px solid var(--border)', borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 18 }}>
          <FileText size={22} color="var(--ink-4)" />
        </div>
        <h3 style={{ fontSize: 15, fontWeight: 600, color: 'var(--ink)', marginBottom: 8 }}>No specs uploaded yet</h3>
        <p style={{ fontSize: 13, color: 'var(--ink-4)', maxWidth: 320, lineHeight: 1.6, marginBottom: 24 }}>
          Upload your first OpenAPI 3.0 spec to start chatting with your API documentation.
        </p>
        <button className="btn-primary" style={{ fontSize: 13 }}>
          <Upload size={14} /> Upload your first spec
        </button>
      </div>
    </div>
  )
}

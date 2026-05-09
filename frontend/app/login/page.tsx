'use client'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { Eye, EyeOff, MessageSquareCode, Loader2 } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '@/lib/api'
import { useAuthStore } from '@/lib/authStore'

const schema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
})
type FormData = z.infer<typeof schema>

export default function LoginPage() {
  const [showPw, setShowPw] = useState(false)
  const router = useRouter()
  const setAuth = useAuthStore((s) => s.setAuth)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const onSubmit = async (data: FormData) => {
    try {
      const res = await api.post('/api/auth/login', data)
      const { accessToken, userId, tenantId, role } = res.data.data
      setAuth(accessToken, userId, tenantId, role)
      toast.success('Welcome back!')
      router.push('/dashboard')
    } catch (err: any) {
      toast.error(err?.response?.data?.error?.code === 'INVALID_CREDENTIALS'
        ? 'Invalid email or password.' : 'Something went wrong.')
    }
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--paper-2)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <div style={{ width: '100%', maxWidth: 400 }} className="animate-scale-in">

        {/* Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 9, justifyContent: 'center', marginBottom: 36 }}>
          <div style={{ width: 32, height: 32, background: 'var(--ink)', borderRadius: 9, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <MessageSquareCode size={16} color="#fff" />
          </div>
          <span style={{ fontWeight: 600, fontSize: 15, color: 'var(--ink)', letterSpacing: '-0.02em' }}>DevDocs AI</span>
        </div>

        <div className="card" style={{ padding: '36px 32px', boxShadow: '0 2px 16px rgba(0,0,0,0.06)' }}>
          <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--ink)', letterSpacing: '-0.025em', marginBottom: 6 }}>Welcome back</h1>
          <p style={{ fontSize: 13, color: 'var(--ink-4)', marginBottom: 28 }}>Sign in to your workspace.</p>

          <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div>
              <label style={{ fontSize: 12, fontWeight: 500, color: 'var(--ink-3)', display: 'block', marginBottom: 6 }}>Email</label>
              <input {...register('email')} type="email" placeholder="you@company.com" className="input-base" />
              {errors.email && <p style={{ fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.email.message}</p>}
            </div>

            <div>
              <label style={{ fontSize: 12, fontWeight: 500, color: 'var(--ink-3)', display: 'block', marginBottom: 6 }}>Password</label>
              <div style={{ position: 'relative' }}>
                <input {...register('password')} type={showPw ? 'text' : 'password'} placeholder="Your password" className="input-base" style={{ paddingRight: 42 }} />
                <button type="button" onClick={() => setShowPw(!showPw)}
                  style={{ position: 'absolute', right: 13, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--ink-4)', display: 'flex' }}>
                  {showPw ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
              {errors.password && <p style={{ fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.password.message}</p>}
            </div>

            <button type="submit" disabled={isSubmitting} className="btn-primary" style={{ width: '100%', padding: '11px', marginTop: 4, fontSize: 14 }}>
              {isSubmitting ? <><Loader2 size={14} className="animate-spin" /> Signing in...</> : 'Sign in →'}
            </button>
          </form>

          <p style={{ textAlign: 'center', fontSize: 13, color: 'var(--ink-4)', marginTop: 24 }}>
            No account?{' '}
            <Link href="/register" style={{ color: 'var(--accent)', fontWeight: 500, textDecoration: 'none' }}>Sign up free</Link>
          </p>
        </div>
      </div>
    </div>
  )
}

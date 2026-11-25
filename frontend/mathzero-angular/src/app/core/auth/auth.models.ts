import { EducationCycle } from '../experience/education-cycle';
import { Language } from '../i18n/translations';

export type UserRole = 'STUDENT' | 'CONTENT_EDITOR' | 'REVIEWER' | 'ADMIN';

export interface StudentProfile {
  readonly id: string;
  readonly displayName: string;
  readonly gradeId: string | null;
  readonly gradeLabel: string | null;
  readonly cycle: EducationCycle | null;
  readonly locale: Language;
  readonly avatarUrl: string | null;
}

export interface AuthUser {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly roles: readonly UserRole[];
  readonly profile: StudentProfile | null;
}

export interface AuthResponse {
  readonly accessToken: string;
  readonly expiresIn: number;
  readonly user: AuthUser;
}

export interface RegisterRequest {
  readonly email: string;
  readonly password: string;
  readonly displayName: string;
  readonly gradeId?: string;
  readonly gradeLabel?: string;
  readonly cycle?: EducationCycle;
  readonly locale?: Language;
}

export interface LoginRequest {
  readonly email: string;
  readonly password: string;
}

export interface ProfileRequest {
  readonly displayName?: string;
  readonly gradeId?: string;
  readonly gradeLabel?: string;
  readonly cycle?: EducationCycle;
  readonly locale?: Language;
  readonly avatarUrl?: string;
}


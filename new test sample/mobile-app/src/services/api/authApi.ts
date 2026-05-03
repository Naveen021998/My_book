import {API_CONFIG} from './config';
import {AuthTokens, LoginInput, RegisterInput} from '../../features/auth/types';

type ErrorPayload = {
  message?: string;
  details?: string[];
};

async function parseError(response: Response): Promise<string> {
  try {
    const payload = (await response.json()) as ErrorPayload;
    if (payload.details && payload.details.length > 0) {
      return payload.details.join(', ');
    }
    return payload.message ?? 'Request failed.';
  } catch {
    return 'Request failed.';
  }
}

export async function register(input: RegisterInput): Promise<AuthTokens> {
  const response = await fetch(`${API_CONFIG.baseUrl}/api/v1/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  return (await response.json()) as AuthTokens;
}

export async function login(input: LoginInput): Promise<AuthTokens> {
  const response = await fetch(`${API_CONFIG.baseUrl}/api/v1/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  return (await response.json()) as AuthTokens;
}

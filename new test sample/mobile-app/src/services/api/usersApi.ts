import {API_CONFIG} from './config';
import {CreateUserInput, User} from '../../features/users/types';

type ErrorPayload = {
  message?: string;
  details?: string[];
};

type PagedResponse<T> = {
  items: T[];
  totalItems: number;
  totalPages: number;
  page: number;
  size: number;
  hasNext: boolean;
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

export async function createUser(input: CreateUserInput): Promise<User> {
  const response = await fetch(`${API_CONFIG.baseUrl}/api/v1/users`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  return (await response.json()) as User;
}

export async function getUsers(): Promise<User[]> {
  const response = await fetch(`${API_CONFIG.baseUrl}/api/v1/users`);

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  const payload = (await response.json()) as PagedResponse<User>;
  return payload.items;
}

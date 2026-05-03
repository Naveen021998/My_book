export type User = {
  id: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  status: string;
  createdAt: string;
};

export type CreateUserInput = {
  fullName: string;
  email: string;
  phoneNumber: string;
};

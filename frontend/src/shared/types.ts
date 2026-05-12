export type Role = 'ADMIN' | 'MANAGER' | 'TRAINER' | 'CLIENT';

export type User = {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  role: Role;
  enabled: boolean;
};

export type Client = {
  id: number;
  user: User;
  registrationDate: string;
  rfidCard?: string;
  birthDate?: string;
};

export type MembershipType = {
  id: number;
  name: string;
  durationDays: number;
  visitCount?: number;
  price: number;
  description?: string;
  active: boolean;
};

export type Schedule = {
  id: number;
  trainingTypeId: number;
  trainingTypeName: string;
  trainerId: number;
  trainerName: string;
  hallId: number;
  hallName: string;
  date: string;
  startTime: string;
  endTime: string;
  participantLimit: number;
  status: string;
  bookedCount: number;
  trainerComment?: string;
};

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
  freezeAllowed: boolean;
  maxFreezeDays: number;
};

export type Membership = {
  id: number;
  clientId: number;
  clientName: string;
  membershipTypeId: number;
  typeName: string;
  purchaseDate: string;
  activationDate: string;
  expirationDate: string;
  remainingVisits?: number;
  status: 'ACTIVE' | 'EXPIRED' | 'FROZEN' | 'CANCELLED' | 'DEPLETED';
  freezeAllowed: boolean;
  maxFreezeDays: number;
  usedFreezeDays: number;
};

export type MembershipFreeze = {
  id: number;
  membershipId: number;
  startDate: string;
  endDate: string;
  reason?: string;
  status: 'SCHEDULED' | 'ACTIVE' | 'FINISHED' | 'CANCELLED';
};

export type Trainer = {
  id: number;
  user: User;
  specialization?: string;
  yearsOfExperience: number;
  description?: string;
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
  status: 'PLANNED' | 'COMPLETED' | 'CANCELLED';
  bookedCount: number;
  trainerComment?: string;
};

export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'SMS' | 'PUSH';
export type NotificationDeliveryStatus = 'SCHEDULED' | 'SENT' | 'FAILED';

export type Notification = {
  id: number;
  title: string;
  message: string;
  type: 'BOOKING' | 'SCHEDULE' | 'MEMBERSHIP' | 'SYSTEM';
  channel: NotificationChannel;
  deliveryStatus: NotificationDeliveryStatus;
  readStatus: boolean;
  createdAt: string;
  sentAt?: string;
};

export type PurchaseRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export type MembershipPurchaseRequest = {
  id: number;
  clientId: number;
  clientName: string;
  membershipTypeId: number;
  membershipTypeName: string;
  price: number;
  createdAt: string;
  desiredActivationDate?: string;
  comment?: string;
  status: PurchaseRequestStatus;
  decidedAt?: string;
  decisionBy?: string;
  decisionComment?: string;
  createdMembershipId?: number;
};

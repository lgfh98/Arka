export interface NotificationRecord {
  id: string;
  recipient: string;
  subject: string;
  message: string;
  channel: string;
  eventType: string;
  sentAt: string;
}

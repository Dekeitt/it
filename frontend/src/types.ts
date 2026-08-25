export type Me = { id: number; issuer: string; subject: string; email: string; roles: string[] };
export type Cleaner = { id?: number; email: string; name: string; rating?: number };
export type AvailabilitySlot = { id?: number; cleanerEmail?: string; dayOfWeek: 'MONDAY'|'TUESDAY'|'WEDNESDAY'|'THURSDAY'|'FRIDAY'|'SATURDAY'|'SUNDAY'; startTime: string; endTime: string; zoneId: string };
export type Job = { id:number; clientEmail?:string; cleanerEmail?:string; status:string; title?:string; description:string; priceCents:number; createdAt?:string; updatedAt?:string };
export type Reservation = { id:number; jobId:number; clientEmail:string; cleanerEmail:string; startAt:string; endAt?:string; durationMinutes:number; agreedAmountCents:number; currency:string; status:string };
export type Payment = { id:number; reservationId:number; amountCents:number; currency:string; stripePaymentIntentId?:string; status:string; createdAt?:string; updatedAt?:string };
export type PaymentIntentResponse = { paymentId:number; clientSecret:string; amountCents:number; currency:string; status:string; publishableKey:string };
export type Review = { id:number; reservationId?:number; cleanerEmail:string; clientEmail:string; rating:number; comment?:string; createdAt?:string };

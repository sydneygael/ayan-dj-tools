import { trigger, transition, query, style, animate } from '@angular/animations';

export const routeAnimations = trigger('routeAnimations', [
  transition('* <=> *', [
    query(
      ':enter',
      [
        style({ opacity: 0, transform: 'translateY(8px)' }),
        animate('200ms ease-out', style({ opacity: 1, transform: 'none' })),
      ],
      { optional: true },
    ),
    query(':leave', [animate('150ms ease-in', style({ opacity: 0 }))], { optional: true }),
  ]),
]);

export const fadeIn = trigger('fadeIn', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateY(4px)' }),
    animate('180ms ease-out', style({ opacity: 1, transform: 'none' })),
  ]),
]);

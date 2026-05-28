import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-chat-tool-call-card',
  imports: [CommonModule],
  template: `
    <div class="tool-card" [class.tool-error]="status() === 'error'">
      <div class="head">
        <span class="dot" [class.run]="status() === 'running'" [class.ok]="status() === 'done'" [class.ko]="status() === 'error'"></span>
        <span class="label">Ayan a utilisé</span>
        <code class="name">{{ name() }}</code>
        @if (status() === 'running') {
          <span class="small">…</span>
        }
      </div>
      @if (args()) {
        <pre class="args">{{ args() }}</pre>
      }
      @if (result()) {
        <details class="result">
          <summary>Résultat</summary>
          <pre>{{ result() }}</pre>
        </details>
      }
    </div>
  `,
  styles: `
    .tool-card { margin: 6px 0; padding: 8px 10px; border-radius: 6px; background: var(--surface-0); border-left: 3px solid var(--accent-text); font-size: .88rem; }
    .tool-card.tool-error { border-left-color: var(--error, #b00); }
    .head { display: flex; align-items: center; gap: 8px; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--muted); }
    .dot.run { background: var(--accent-text); animation: pulse 1s infinite; }
    .dot.ok { background: var(--success, #2a8); }
    .dot.ko { background: var(--error, #b00); }
    .label { color: var(--muted); }
    .name { font-weight: 600; color: var(--accent-text); }
    .args, .result pre { margin: 4px 0 0; padding: 6px; background: var(--surface-2); border-radius: 4px; font-size: .8rem; overflow-x: auto; white-space: pre-wrap; word-break: break-all; }
    .result { margin-top: 4px; }
    .result summary { cursor: pointer; font-size: .8rem; color: var(--muted); }
    .small { color: var(--muted); font-size: .8rem; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: .4; } }
  `
})
export class ChatToolCallCardComponent {
  readonly name = input.required<string>();
  readonly args = input<string | undefined>();
  readonly status = input<'running' | 'done' | 'error'>('done');
  readonly result = input<string | undefined>();
}

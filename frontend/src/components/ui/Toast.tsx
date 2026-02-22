import { useEffect, useState } from 'react';
import { CheckCircle, XCircle, X } from 'lucide-react';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error';
  text: string;
}

let toastId = 0;
let addToastFn: ((msg: Omit<ToastMessage, 'id'>) => void) | null = null;

export function toast(type: 'success' | 'error', text: string) {
  addToastFn?.({ type, text });
}

export default function ToastContainer() {
  const [messages, setMessages] = useState<ToastMessage[]>([]);

  useEffect(() => {
    addToastFn = (msg) => {
      const id = ++toastId;
      setMessages((prev) => [...prev, { ...msg, id }]);
      setTimeout(() => setMessages((prev) => prev.filter((m) => m.id !== id)), 4000);
    };
    return () => { addToastFn = null; };
  }, []);

  const remove = (id: number) => setMessages((prev) => prev.filter((m) => m.id !== id));

  if (messages.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-[100] space-y-2 max-w-sm">
      {messages.map((m) => (
        <div
          key={m.id}
          className={`flex items-center gap-2 px-4 py-3 rounded-lg shadow-lg text-sm font-medium text-white animate-slide-in ${
            m.type === 'success' ? 'bg-emerald-600' : 'bg-red-600'
          }`}
        >
          {m.type === 'success' ? <CheckCircle className="w-4 h-4 flex-shrink-0" /> : <XCircle className="w-4 h-4 flex-shrink-0" />}
          <span className="flex-1">{m.text}</span>
          <button onClick={() => remove(m.id)} className="p-0.5 hover:bg-white/20 rounded">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      ))}
    </div>
  );
}

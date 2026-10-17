import { useEffect, useRef, useState } from "react";

const MIME = { pdf: "application/pdf", md: "text/markdown", txt: "text/plain" };
const withMime = (file, ext) => new File([file], file.name, { type: MIME[ext] });

async function readError(res) {
  try {
    const j = await res.json();
    return j.message || j.error || `Request failed (${res.status}).`;
  } catch {
    return `Request failed (${res.status}).`;
  }
}

const friendly = (e) =>
  e instanceof TypeError
    ? "Can't reach the server. Check that the backend is running, then try again."
    : e.message;

// Backend edges look like: "Priya —DECIDED→ Move session storage ..."
const EDGE = /^(.+?) —([A-Z_]+)→ (.+)$/;

function Edge({ text }) {
  const m = text.match(EDGE);
  if (!m) return <li className="text-[#8a8a8a]">{text}</li>;
  return (
    <li className="flex flex-wrap items-center gap-x-2 gap-y-1">
      <span>{m[1]}</span>
      <span className="rounded bg-[#1f1f1f] px-1.5 py-0.5 text-xs font-medium text-emerald-300">{m[2]}</span>
      <span>{m[3]}</span>
    </li>
  );
}

function Path({ path }) {
  if (!path?.length) return null;
  return (
    <details open className="mt-4 rounded-xl border border-[#2a2a2a] bg-[#111] px-4 py-3 text-sm">
      <summary className="cursor-pointer select-none font-medium text-[#b5b5b5] focus-visible:outline-2 focus-visible:outline-[#6a6a6a]">
        How I got this
      </summary>
      <div className="mt-3 space-y-4">
        {path.map((s, i) => (
          <div key={i}>
            <p className="text-xs text-[#8a8a8a]">
              {i + 1}. {s.tool === "searchDocuments" ? "Searched document text" : "Followed the graph"} · {s.tool}("{s.input}")
            </p>
            {s.edges?.length > 0 ? (
              <ul className="mt-2 space-y-1.5 leading-6">
                {s.edges.map((e, j) => <Edge key={j} text={e} />)}
              </ul>
            ) : (
              <p className="mt-2 text-[#8a8a8a]">Nothing found.</p>
            )}
          </div>
        ))}
      </div>
    </details>
  );
}

function Message({ m, onRetry }) {
  if (m.role === "user") {
    return (
      <div className="flex justify-end">
        <p className="max-w-[85%] whitespace-pre-wrap rounded-2xl bg-[#1f1f1f] px-4 py-2.5 leading-7">{m.text}</p>
      </div>
    );
  }
  if (m.error) {
    return (
      <div role="alert" className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3">
        <p className="text-sm font-medium text-red-300">Couldn't get an answer</p>
        <p className="mt-1 text-[15px] leading-7 text-red-200/90">{m.text}</p>
        <button
          onClick={onRetry}
          className="mt-2 rounded-md border border-red-400/30 px-3 py-1 text-sm text-red-200 transition-colors hover:bg-red-500/15 focus-visible:outline-2 focus-visible:outline-red-300"
        >
          Try again
        </button>
      </div>
    );
  }
  return (
    <div>
      <p className="whitespace-pre-wrap text-[16px] leading-7">{m.text}</p>
      <Path path={m.path} />
    </div>
  );
}

const EXAMPLES = [
  "Why did we stop using Redis for sessions?",
  "Who should I talk to about the billing service?",
  "What decisions affect the auth service, and are any outdated?",
];

export default function App() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [notice, setNotice] = useState(null);
  const endRef = useRef(null);
  const taRef = useRef(null);
  const fileRef = useRef(null);
  const timer = useRef(null);

  useEffect(() => { endRef.current?.scrollIntoView({ behavior: "smooth" }); }, [messages, busy]);

  useEffect(() => {
    const ta = taRef.current;
    if (!ta) return;
    ta.style.height = "auto";
    ta.style.height = Math.min(ta.scrollHeight, 200) + "px";
  }, [input]);

  const show = (type, text) => {
    clearTimeout(timer.current);
    setNotice({ type, text });
    if (type !== "info") timer.current = setTimeout(() => setNotice(null), 6000);
  };

  async function upload(e) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    const ext = file.name.split(".").pop().toLowerCase();
    if (!MIME[ext]) {
      show("error", "Only PDF, Markdown (.md), and text (.txt) files are supported.");
      return;
    }
    setUploading(true);
    show("info", `Uploading ${file.name}…`);
    const body = new FormData();
    body.append("multipartFile", withMime(file, ext));
    try {
      const res = await fetch("/documents", { method: "POST", body });
      if (!res.ok) throw new Error(await readError(res));
      show("ok", `${file.name} received. Indexing takes a few seconds.`);
    } catch (err) {
      show("error", friendly(err));
    } finally {
      setUploading(false);
    }
  }

  async function send(q, retry = false) {
    if (!q || busy) return;
    setMessages((m) => (retry ? m.slice(0, -1) : [...m, { role: "user", text: q }]));
    setBusy(true);
    try {
      const res = await fetch(`/ask?question=${encodeURIComponent(q)}`);
      if (!res.ok) throw new Error(await readError(res));
      const data = await res.json();
      setMessages((m) => [...m, { role: "assistant", text: data.answer, path: data.path }]);
    } catch (err) {
      setMessages((m) => [...m, { role: "assistant", text: friendly(err), error: true, q }]);
    } finally {
      setBusy(false);
    }
  }

  function submit(e) {
    e.preventDefault();
    const q = input.trim();
    if (!q) return;
    setInput("");
    send(q);
  }

  const noticeColor =
    notice?.type === "error" ? "text-red-300" : notice?.type === "ok" ? "text-emerald-300" : "text-[#8a8a8a]";

  return (
    <div className="flex h-full flex-col">
      <header className="px-5 py-4 text-sm text-[#8a8a8a]">Decision Tracker</header>

      <main className="flex-1 overflow-y-auto" aria-live="polite">
        <div className="mx-auto flex min-h-full max-w-3xl flex-col gap-6 px-5 pb-6">
          {messages.length === 0 && (
            <div className="my-auto py-16 text-center">
              <h1 className="text-3xl font-medium tracking-tight">Why was that decided?</h1>
              <p className="mt-3 text-[#8a8a8a]">Ask about decisions, owners, and incidents. Every answer shows the path behind it.</p>
              <div className="mt-8 flex flex-col items-center gap-2">
                {EXAMPLES.map((ex) => (
                  <button
                    key={ex}
                    onClick={() => send(ex)}
                    className="rounded-xl border border-[#2a2a2a] px-4 py-2 text-sm text-[#d0d0d0] transition-colors hover:bg-[#161616] focus-visible:outline-2 focus-visible:outline-[#6a6a6a]"
                  >
                    {ex}
                  </button>
                ))}
              </div>
            </div>
          )}
          {messages.map((m, i) => (
            <Message key={i} m={m} onRetry={() => send(m.q, true)} />
          ))}
          {busy && <p className="text-[#8a8a8a]">Following the graph…</p>}
          <div ref={endRef} />
        </div>
      </main>

      <footer className="px-5 pb-5">
        <div className="mx-auto max-w-3xl">
          {notice && <p role="status" className={`mb-2 px-1 text-sm ${noticeColor}`}>{notice.text}</p>}
          <form
            onSubmit={submit}
            className="flex items-end gap-2 rounded-2xl border border-[#2a2a2a] bg-[#161616] p-2 focus-within:border-[#4a4a4a]"
          >
            <input ref={fileRef} type="file" accept=".pdf,.md,.txt" className="sr-only" onChange={upload} />
            <button
              type="button"
              onClick={() => fileRef.current?.click()}
              disabled={uploading}
              aria-label="Add document (PDF, Markdown, or text)"
              title="Add document (PDF, Markdown, or text)"
              className="flex size-9 shrink-0 items-center justify-center rounded-lg text-xl text-[#b5b5b5] transition-colors hover:bg-[#242424] hover:text-white disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-[#6a6a6a]"
            >
              +
            </button>
            <textarea
              ref={taRef}
              rows={1}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) submit(e); }}
              placeholder="Ask a question"
              aria-label="Question"
              className="max-h-[200px] flex-1 resize-none bg-transparent px-1 py-2 text-[16px] leading-6 outline-none placeholder:text-[#6a6a6a]"
            />
            <button
              type="submit"
              disabled={busy || !input.trim()}
              aria-label="Send"
              className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-white text-black transition-opacity disabled:opacity-30 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              ↑
            </button>
          </form>
        </div>
      </footer>
    </div>
  );
}

import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Calendar, Mail, CheckCircle2, Circle, Sparkles, PlusCircle, LogOut, Clock } from 'lucide-react';

export default function Dashboard({ token }) {
    const [tasks, setTasks] = useState([]);
    const [calendarEvents, setCalendarEvents] = useState([]);
    const [emailSnippets, setEmailSnippets] = useState([]);
    const [manualTitle, setManualTitle] = useState('');
    const [nlpPrompt, setNlpPrompt] = useState('');
    const [loadingContext, setLoadingContext] = useState(true);
    const [submittingNlp, setSubmittingNlp] = useState(false);

    // Configure standard Axios defaults to securely pass our Bearer JWT on every single execution
    const apiOptions = {
        headers: { Authorization: `Bearer ${token}` }
    };

    // --- Core API Handlers ---
    const fetchDashboardData = async () => {
        try {
            // 1. Fetch Local DB Tasks
            const tasksRes = await axios.get('http://localhost:8080/api/tasks', apiOptions);
            setTasks(tasksRes.data);

            // 2. Fetch Google Ecosystem Context Insights
            const contextRes = await axios.get('http://localhost:8080/api/assistant/dashboard-context', apiOptions);
            setCalendarEvents(contextRes.data.calendarEvents || []);
            setEmailSnippets(contextRes.data.unreadEmailSnippets || []);
        } catch (err) {
            console.error("Error communicating with backend modules:", err);
        } finally {
            setLoadingContext(false);
        }
    };

    // Hook to fetch data immediately on mount and establish background updates automatically
    useEffect(() => {
        fetchDashboardData();
        const autoSyncTimer = setInterval(() => {
            fetchDashboardData();
        }, 10000);

        return () => clearInterval(autoSyncTimer);
    }, []);

    const handleCreateManualTask = async (e) => {
        e.preventDefault();
        if (!manualTitle.trim()) return;
        try {
            const res = await axios.post('http://localhost:8080/api/tasks', {
                title: manualTitle,
                description: 'Manually generated item'
            }, apiOptions);
            setTasks([...tasks, res.data]);
            setManualTitle('');
        } catch (err) {
            console.error(err);
        }
    };

    const handleCreateNlpTask = async (e) => {
        e.preventDefault();
        if (!nlpPrompt.trim()) return;
        setSubmittingNlp(true);
        try {
            const res = await axios.post('http://localhost:8080/api/tasks/nlp', {
                text: nlpPrompt
            }, apiOptions);
            setTasks([...tasks, res.data]);
            setNlpPrompt('');
        } catch (err) {
            console.error(err);
        } finally {
            setSubmittingNlp(false);
        }
    };

    const handleToggleTask = async (id) => {
        try {
            const res = await axios.put(`http://localhost:8080/api/tasks/${id}/toggle`, {}, apiOptions);
            setTasks(tasks.map(t => t.id === id ? res.data : t));
        } catch (err) {
            console.error(err);
        }
    };

    const handleSignOut = () => {
        localStorage.removeItem('assistant_jwt');
        window.location.href = '/login';
    };

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100 font-sans antialiased">
            {/* --- Top Dashboard Header Navigation --- */}
            <header className="border-b border-slate-900 bg-slate-900/40 backdrop-blur-md sticky top-0 z-50">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="bg-indigo-600 p-2 rounded-xl text-white shadow-lg shadow-indigo-600/20">
                            <Sparkles className="w-5 h-5 animate-pulse" />
                        </div>
                        <div>
                            <h1 className="text-xl font-bold tracking-tight bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent">Context AI</h1>
                            <p className="text-xs font-medium text-slate-500">Workspace Management Hub</p>
                        </div>
                    </div>
                    <button
                        onClick={handleSignOut}
                        className="flex items-center gap-2 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-semibold px-4 py-2 rounded-xl transition-all shadow-sm cursor-pointer"
                    >
                        <LogOut className="w-3.5 h-3.5 text-slate-400" />
                        Sign Out
                    </button>
                </div>
            </header>

            {/* --- Main Workspace Content Grid Area --- */}
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

                    {/* --- Column 1 & 2: Task Generation & Management Pipeline --- */}
                    <div className="lg:col-span-2 space-y-8">

                        {/* AI Natural Language Processing Window */}
                        <section className="bg-gradient-to-br from-slate-900 to-slate-900/70 border border-slate-800 rounded-2xl p-6 shadow-xl relative overflow-hidden">
                            <div className="absolute top-0 right-0 w-32 h-32 bg-indigo-500/5 blur-3xl rounded-full"></div>
                            <div className="flex items-center gap-2 mb-4 text-indigo-400">
                                <Sparkles className="w-4 h-4" />
                                <h2 className="text-sm font-bold uppercase tracking-wider text-indigo-400">AI Context Engine Parsing</h2>
                            </div>
                            <p className="text-sm text-slate-400 mb-4 leading-relaxed">
                                Paste a raw email snippet, meeting minutes, or quick text reminders below. Your local model will extract actionable targets automatically.
                            </p>
                            <form onSubmit={handleCreateNlpTask} className="space-y-3">
                                <textarea
                                    value={nlpPrompt}
                                    onChange={(e) => setNlpPrompt(e.target.value)}
                                    placeholder="e.g., Review the document shared by Dr. Sairaj by tomorrow at 4 PM and submit comments on the architecture diagram."
                                    className="w-full h-24 bg-slate-950 border border-slate-800 rounded-xl p-3.5 text-sm text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500 transition-colors resize-none"
                                />
                                <button
                                    type="submit"
                                    disabled={submittingNlp || !nlpPrompt.trim()}
                                    className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-800 disabled:text-slate-600 disabled:cursor-not-allowed text-white font-semibold text-sm py-2.5 px-4 rounded-xl transition-all cursor-pointer shadow-lg shadow-indigo-600/10"
                                >
                                    {submittingNlp ? (
                                        <>
                                            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                                            Ollama Local Core Querying...
                                        </>
                                    ) : (
                                        <>Parse Text With AI</>
                                    )}
                                </button>
                            </form>
                        </section>


                        <section className="bg-slate-900/40 border border-slate-900 rounded-2xl p-6 shadow-md">
                            <div className="flex items-center justify-between mb-6">
                                <h2 className="text-lg font-bold text-white tracking-tight">Active Work Tasks ({tasks.length})</h2>

                                <form onSubmit={handleCreateManualTask} className="flex gap-2 max-w-xs w-full">
                                    <input
                                        type="text"
                                        value={manualTitle}
                                        onChange={(e) => setManualTitle(e.target.value)}
                                        placeholder="Quick add task item..."
                                        className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500 w-full"
                                    />
                                    <button type="submit" className="bg-slate-800 hover:bg-slate-700 text-indigo-400 p-2 rounded-xl border border-slate-700 transition-colors cursor-pointer">
                                        <PlusCircle className="w-4 h-4" />
                                    </button>
                                </form>
                            </div>

                            {tasks.length === 0 ? (
                                <div className="text-center py-12 bg-slate-950/40 rounded-xl border border-dashed border-slate-800">
                                    <p className="text-sm text-slate-500">No active tracking tasks found in database.</p>
                                </div>
                            ) : (
                                <div className="space-y-2.5">
                                    {tasks.map((task) => (
                                        <div
                                            key={task.id}
                                            className={`flex items-start gap-3.5 p-4 rounded-xl border transition-all ${task.status === 'COMPLETED'
                                                ? 'bg-slate-950/20 border-slate-900 text-slate-500'
                                                : 'bg-slate-900/60 border-slate-800/80 text-slate-200 hover:border-slate-700'
                                                }`}
                                        >
                                            <button
                                                onClick={() => handleToggleTask(task.id)}
                                                className="mt-0.5 text-indigo-500 hover:text-indigo-400 transition-colors cursor-pointer"
                                            >
                                                {task.status === 'COMPLETED' ? (
                                                    <CheckCircle2 className="w-5 h-5 text-emerald-500/80 fill-emerald-500/10" />
                                                ) : (
                                                    <Circle className="w-5 h-5 text-slate-600" />
                                                )}
                                            </button>
                                            <div className="flex-1 min-w-0">
                                                <p className={`text-sm font-semibold truncate ${task.status === 'COMPLETED' ? 'line-through text-slate-600' : 'text-slate-200'}`}>
                                                    {task.title}
                                                </p>
                                                {task.description && (
                                                    <p className={`text-xs mt-1 leading-relaxed ${task.status === 'COMPLETED' ? 'text-slate-600' : 'text-slate-400'}`}>
                                                        {task.description}
                                                    </p>
                                                )}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </section>
                    </div>

                    {/* --- Column 3: Google Workspace Context Hub Sidebar --- */}
                    <div className="space-y-8">
                        <section className="bg-slate-900/50 border border-slate-900 rounded-2xl p-5 shadow-sm">
                            <div className="flex items-center gap-2 mb-4 text-amber-500">
                                <Calendar className="w-4 h-4" />
                                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">Google Calendar Agenda</h3>
                            </div>
                            {loadingContext ? (
                                <div className="animate-pulse space-y-2 py-4"><div className="h-4 bg-slate-800 rounded w-3/4"></div><div className="h-4 bg-slate-800 rounded w-1/2"></div></div>
                            ) : calendarEvents.length === 0 ? (
                                <p className="text-xs text-slate-600 italic py-2">No upcoming scheduled events found or tokens unlinked.</p>
                            ) : (
                                <div className="space-y-3">
                                    {calendarEvents.map((evt, idx) => (
                                        <div key={idx} className="bg-slate-950 p-3 rounded-xl border border-slate-900 flex flex-col gap-1">
                                            <span className="text-xs font-semibold text-slate-200 line-clamp-1">{evt.summary}</span>
                                            <div className="flex items-center gap-1.5 text-[10px] font-medium text-slate-500">
                                                <Clock className="w-3 h-3 text-slate-600" />
                                                <span>
                                                    {evt.start?.dateTime?.value
                                                        ? new Date(evt.start.dateTime.value).toLocaleString()
                                                        : evt.start?.date
                                                            ? new Date(evt.start.date).toLocaleDateString()
                                                            : 'Scheduled Event'}
                                                </span>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </section>

                        <section className="bg-slate-900/50 border border-slate-900 rounded-2xl p-5 shadow-sm">
                            <div className="flex items-center gap-2 mb-4 text-sky-400">
                                <Mail className="w-4 h-4" />
                                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">Unread Gmail Feed</h3>
                            </div>
                            {loadingContext ? (
                                <div className="animate-pulse space-y-2 py-4"><div className="h-3 bg-slate-800 rounded w-full"></div><div className="h-3 bg-slate-800 rounded w-5/6"></div></div>
                            ) : emailSnippets.length === 0 ? (
                                <p className="text-xs text-slate-600 italic py-2">Inbox up to date. No unread text snippets available.</p>
                            ) : (
                                <div className="space-y-2.5">
                                    {emailSnippets.map((snippet, idx) => (
                                        <div key={idx} className="bg-slate-950/80 p-3 rounded-xl border border-slate-900/60 hover:border-slate-800 transition-colors">
                                            <p className="text-xs leading-relaxed text-slate-400 line-clamp-2 italic">
                                                "{snippet}"
                                            </p>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </section>
                    </div>

                </div>
            </main>
        </div>
    );
}
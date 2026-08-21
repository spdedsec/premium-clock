/**
 * Design reminder — Chronographic Modernism:
 * This is a functional instrument, not a generic dashboard. Make time dominant,
 * use progressive disclosure for advanced utilities, and reserve Signal Vermilion for live states.
 */
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlarmClock,
  ArrowRight,
  CalendarDays,
  Check,
  ChevronDown,
  Clock3,
  Download,
  Globe2,
  Minus,
  MoreHorizontal,
  Moon,
  Pause,
  Play,
  Plus,
  RotateCcw,
  Search,
  Settings2,
  Square,
  Sun,
  TimerReset,
  Trash2,
  X,
  Zap,
} from "lucide-react";

type ViewKey = "clock" | "alarms" | "timers" | "tools" | "settings";
type ThemeMode = "light" | "dark" | "system";
type ClockStyle = "Large" | "Compact" | "Editorial" | "Analog" | "Mono";

type Alarm = {
  id: string;
  time: string;
  label: string;
  enabled: boolean;
  repeat: string[];
  createdAt: number;
};

type Timer = {
  id: string;
  name: string;
  duration: number;
  remaining: number;
  running: boolean;
  endsAt?: number;
};

type Stopwatch = {
  running: boolean;
  elapsed: number;
  startedAt?: number;
  laps: number[];
};

type WorldCity = { id: string; name: string; zone: string; region: string };
type HistoryEntry = { id: string; label: string; at: number };
type Analytics = { alarmsCreated: number; timersStarted: number; timersCompleted: number; stopwatchSessions: number; focusSessions: number };
type AppSettings = { theme: ThemeMode; showSeconds: boolean; hour24: boolean; accent: string; clockStyle: ClockStyle };

const STORAGE = "premium-clock-v1";
const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const clockStyles: ClockStyle[] = ["Large", "Compact", "Editorial", "Analog", "Mono"];
const cityCatalog: WorldCity[] = [
  { id: "new-delhi", name: "New Delhi", zone: "Asia/Kolkata", region: "India" },
  { id: "london", name: "London", zone: "Europe/London", region: "United Kingdom" },
  { id: "new-york", name: "New York", zone: "America/New_York", region: "United States" },
  { id: "tokyo", name: "Tokyo", zone: "Asia/Tokyo", region: "Japan" },
  { id: "sydney", name: "Sydney", zone: "Australia/Sydney", region: "Australia" },
  { id: "reykjavik", name: "Reykjavík", zone: "Atlantic/Reykjavik", region: "Iceland" },
  { id: "singapore", name: "Singapore", zone: "Asia/Singapore", region: "Singapore" },
];

const defaultSettings: AppSettings = { theme: "light", showSeconds: true, hour24: false, accent: "#D6472D", clockStyle: "Large" };
const defaultAnalytics: Analytics = { alarmsCreated: 0, timersStarted: 0, timersCompleted: 0, stopwatchSessions: 0, focusSessions: 0 };
const defaultStopwatch: Stopwatch = { running: false, elapsed: 0, laps: [] };

function readStored<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(`${STORAGE}-${key}`);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function writeStored<T>(key: string, value: T) {
  try {
    window.localStorage.setItem(`${STORAGE}-${key}`, JSON.stringify(value));
  } catch {
    // The interface continues to work in-memory if browser storage is unavailable.
  }
}

function makeId(prefix: string) {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;
}

function formatDuration(milliseconds: number, includeTenths = false) {
  const total = Math.max(0, milliseconds);
  const hours = Math.floor(total / 3_600_000);
  const minutes = Math.floor((total % 3_600_000) / 60_000);
  const seconds = Math.floor((total % 60_000) / 1_000);
  const tenths = Math.floor((total % 1_000) / 100);
  const core = hours > 0 ? `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}` : `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  return includeTenths ? `${core}.${tenths}` : core;
}

function timeForZone(date: Date, timeZone: string, hour24: boolean) {
  return new Intl.DateTimeFormat([], { timeZone, hour: "2-digit", minute: "2-digit", hour12: !hour24 }).format(date);
}

function dateForZone(date: Date, timeZone: string) {
  return new Intl.DateTimeFormat([], { timeZone, weekday: "short", month: "short", day: "numeric" }).format(date);
}

function zoneLabel(timeZone: string) {
  try {
    const short = new Intl.DateTimeFormat([], { timeZone, timeZoneName: "shortOffset" }).formatToParts(new Date()).find((part) => part.type === "timeZoneName")?.value;
    return short ?? timeZone;
  } catch {
    return timeZone;
  }
}

function isNight(date: Date, timeZone: string) {
  const hour = Number(new Intl.DateTimeFormat("en-US", { timeZone, hour: "2-digit", hourCycle: "h23" }).format(date));
  return hour < 6 || hour >= 19;
}

function getNextAlarm(alarms: Alarm[]) {
  const enabled = alarms.filter((alarm) => alarm.enabled);
  if (!enabled.length) return null;
  const now = new Date();
  let closest: { alarm: Alarm; at: Date } | null = null;

  for (const alarm of enabled) {
    const [hours, minutes] = alarm.time.split(":").map(Number);
    for (let offset = 0; offset <= 7; offset += 1) {
      const candidate = new Date(now);
      candidate.setDate(now.getDate() + offset);
      candidate.setHours(hours, minutes, 0, 0);
      const allowed = alarm.repeat.length === 0 || alarm.repeat.includes(days[candidate.getDay()]);
      if (allowed && candidate.getTime() > now.getTime()) {
        if (!closest || candidate.getTime() < closest.at.getTime()) closest = { alarm, at: candidate };
        break;
      }
    }
  }
  return closest;
}

function timeUntilLabel(target: Date) {
  const delta = Math.max(0, target.getTime() - Date.now());
  const hours = Math.floor(delta / 3_600_000);
  const minutes = Math.floor((delta % 3_600_000) / 60_000);
  if (hours === 0) return `in ${minutes} min`;
  return `in ${hours}h ${minutes}m`;
}

function AnalogClock({ date }: { date: Date }) {
  const hour = ((date.getHours() % 12) + date.getMinutes() / 60) * 30;
  const minute = (date.getMinutes() + date.getSeconds() / 60) * 6;
  const second = date.getSeconds() * 6;
  return (
    <div className="analog-face" style={{ "--hour": `${hour}deg`, "--minute": `${minute}deg`, "--second": `${second}deg` } as React.CSSProperties} aria-label="Current analog time">
      {Array.from({ length: 60 }, (_, index) => <span key={index} className={`analog-tick ${index % 5 === 0 ? "major" : ""}`} style={{ transform: `translateX(-50%) rotate(${index * 6}deg)` }} />)}
      <span className="analog-hand hour" />
      <span className="analog-hand minute" />
      <span className="analog-hand second" />
      <span className="analog-pin" />
    </div>
  );
}

function Toggle({ on, label, onClick }: { on: boolean; label: string; onClick: () => void }) {
  return <button type="button" aria-pressed={on} aria-label={label} className={`toggle ${on ? "is-on" : ""}`} onClick={onClick}><span /></button>;
}

function SectionHeader({ kicker, title, subtitle, action }: { kicker: string; title: string; subtitle: string; action?: React.ReactNode }) {
  return <div className="section-header"><div><p className="eyebrow">{kicker}</p><h1 className="section-title">{title}</h1></div><div>{subtitle && <p className="section-subtitle">{subtitle}</p>}{action}</div></div>;
}

export default function Home() {
  const [now, setNow] = useState(() => new Date());
  const [view, setView] = useState<ViewKey>("clock");
  const [settings, setSettings] = useState<AppSettings>(() => readStored("settings", defaultSettings));
  const [alarms, setAlarms] = useState<Alarm[]>(() => readStored("alarms", []));
  const [timers, setTimers] = useState<Timer[]>(() => readStored("timers", []));
  const [worldCities, setWorldCities] = useState<WorldCity[]>(() => readStored("world-cities", [cityCatalog[0], cityCatalog[1], cityCatalog[2]]));
  const [stopwatch, setStopwatch] = useState<Stopwatch>(() => readStored("stopwatch", defaultStopwatch));
  const [analytics, setAnalytics] = useState<Analytics>(() => readStored("analytics", defaultAnalytics));
  const [history, setHistory] = useState<HistoryEntry[]>(() => readStored("history", []));
  const [alarmTime, setAlarmTime] = useState("07:00");
  const [alarmLabel, setAlarmLabel] = useState("");
  const [alarmRepeat, setAlarmRepeat] = useState<string[]>([]);
  const [timerName, setTimerName] = useState("Custom timer");
  const [timerMinutes, setTimerMinutes] = useState("10");
  const [cityToAdd, setCityToAdd] = useState(cityCatalog[3].id);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [notice, setNotice] = useState("");
  const [conversionTime, setConversionTime] = useState("09:00");
  const [conversionFrom, setConversionFrom] = useState("Asia/Kolkata");
  const [conversionTo, setConversionTo] = useState("Europe/London");

  const notify = useCallback((message: string) => {
    setNotice(message);
    window.setTimeout(() => setNotice(""), 2600);
  }, []);

  const record = useCallback((label: string, counter?: keyof Analytics) => {
    setHistory((current) => [{ id: makeId("event"), label, at: Date.now() }, ...current].slice(0, 40));
    if (counter) setAnalytics((current) => ({ ...current, [counter]: current[counter] + 1 }));
  }, []);

  useEffect(() => {
    const interval = window.setInterval(() => setNow(new Date()), 250);
    return () => window.clearInterval(interval);
  }, []);

  useEffect(() => writeStored("settings", settings), [settings]);
  useEffect(() => writeStored("alarms", alarms), [alarms]);
  useEffect(() => writeStored("timers", timers), [timers]);
  useEffect(() => writeStored("world-cities", worldCities), [worldCities]);
  useEffect(() => writeStored("stopwatch", stopwatch), [stopwatch]);
  useEffect(() => writeStored("analytics", analytics), [analytics]);
  useEffect(() => writeStored("history", history), [history]);

  useEffect(() => {
    const shouldBeDark = settings.theme === "dark" || (settings.theme === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);
    document.documentElement.classList.toggle("dark", shouldBeDark);
    document.documentElement.style.setProperty("--signal", settings.accent);
  }, [settings.theme, settings.accent]);

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setSearchOpen(true);
      }
      if (event.key === "Escape") setSearchOpen(false);
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, []);

  useEffect(() => {
    const interval = window.setInterval(() => {
      let completed = false;
      setTimers((current) => current.map((timer) => {
        if (!timer.running || !timer.endsAt) return timer;
        const remaining = Math.max(0, timer.endsAt - Date.now());
        if (remaining === 0) {
          completed = true;
          return { ...timer, remaining: 0, running: false, endsAt: undefined };
        }
        return { ...timer, remaining };
      }));
      if (completed) {
        record("Timer completed", "timersCompleted");
        notify("Timer complete. Time is yours again.");
      }
    }, 250);
    return () => window.clearInterval(interval);
  }, [notify, record]);

  const nextAlarm = useMemo(() => getNextAlarm(alarms), [alarms, now.getMinutes()]);
  const currentStopwatch = stopwatch.running && stopwatch.startedAt ? stopwatch.elapsed + (now.getTime() - stopwatch.startedAt) : stopwatch.elapsed;
  const dayOfYear = Math.floor((now.getTime() - new Date(now.getFullYear(), 0, 0).getTime()) / 86_400_000);
  const endOfYear = new Date(now.getFullYear() + 1, 0, 1).getTime();
  const daysRemaining = Math.ceil((endOfYear - now.getTime()) / 86_400_000);
  const activeTimers = timers.filter((timer) => timer.running).length;

  const currentTime = new Intl.DateTimeFormat([], { hour: "2-digit", minute: "2-digit", hour12: !settings.hour24 }).format(now);
  const seconds = String(now.getSeconds()).padStart(2, "0");
  const dateLabel = new Intl.DateTimeFormat([], { weekday: "long", month: "long", day: "numeric" }).format(now);
  const localZone = Intl.DateTimeFormat().resolvedOptions().timeZone;

  const switchView = (next: ViewKey) => { setView(next); window.scrollTo({ top: 0, behavior: "smooth" }); };

  const addAlarm = () => {
    const created: Alarm = { id: makeId("alarm"), time: alarmTime, label: alarmLabel.trim() || "Alarm", enabled: true, repeat: alarmRepeat, createdAt: Date.now() };
    setAlarms((current) => [...current, created].sort((a, b) => a.time.localeCompare(b.time)));
    setAlarmLabel(""); setAlarmRepeat([]);
    record(`Alarm created · ${created.time}`, "alarmsCreated");
    notify("Alarm saved locally.");
  };

  const addTimer = (duration: number, name: string, startImmediately = true) => {
    if (!Number.isFinite(duration) || duration <= 0) { notify("Enter a duration greater than zero."); return; }
    const timer: Timer = { id: makeId("timer"), name, duration, remaining: duration, running: startImmediately, endsAt: startImmediately ? Date.now() + duration : undefined };
    setTimers((current) => [timer, ...current]);
    if (startImmediately) record(`Timer started · ${name}`, "timersStarted");
    notify(`${name} ${startImmediately ? "started" : "added"}.`);
  };

  const toggleTimer = (id: string) => {
    setTimers((current) => current.map((timer) => {
      if (timer.id !== id) return timer;
      if (timer.running) return { ...timer, running: false, endsAt: undefined, remaining: timer.endsAt ? Math.max(0, timer.endsAt - Date.now()) : timer.remaining };
      if (timer.remaining === 0) return { ...timer, running: true, remaining: timer.duration, endsAt: Date.now() + timer.duration };
      record(`Timer started · ${timer.name}`, "timersStarted");
      return { ...timer, running: true, endsAt: Date.now() + timer.remaining };
    }));
  };

  const resetTimer = (id: string) => setTimers((current) => current.map((timer) => timer.id === id ? { ...timer, remaining: timer.duration, running: false, endsAt: undefined } : timer));
  const removeTimer = (id: string) => { setTimers((current) => current.filter((timer) => timer.id !== id)); notify("Timer removed."); };
  const toggleAlarm = (id: string) => setAlarms((current) => current.map((alarm) => alarm.id === id ? { ...alarm, enabled: !alarm.enabled } : alarm));
  const removeAlarm = (id: string) => { setAlarms((current) => current.filter((alarm) => alarm.id !== id)); notify("Alarm removed."); };

  const startStopwatch = () => {
    setStopwatch((current) => {
      if (current.running) {
        return { ...current, running: false, elapsed: current.elapsed + (Date.now() - (current.startedAt ?? Date.now())), startedAt: undefined };
      }
      record("Stopwatch session started", "stopwatchSessions");
      return { ...current, running: true, startedAt: Date.now() };
    });
  };
  const lapStopwatch = () => { if (stopwatch.running) setStopwatch((current) => ({ ...current, laps: [currentStopwatch, ...current.laps] })); };
  const resetStopwatch = () => setStopwatch(defaultStopwatch);

  const addWorldCity = () => {
    const city = cityCatalog.find((item) => item.id === cityToAdd);
    if (!city) return;
    if (worldCities.some((item) => item.id === city.id)) { notify(`${city.name} is already on your clock.`); return; }
    setWorldCities((current) => [...current, city]);
    notify(`${city.name} added to world time.`);
  };
  const removeWorldCity = (id: string) => setWorldCities((current) => current.filter((city) => city.id !== id));

  const startFocus = (minutes: number) => {
    addTimer(minutes * 60_000, `${minutes} minute focus`, true);
    setAnalytics((current) => ({ ...current, focusSessions: current.focusSessions + 1 }));
  };

  const searchActions = [
    { title: "Clock", description: "Return to the live local time", view: "clock" as ViewKey },
    { title: "New alarm", description: "Open alarms and create a wake-up time", view: "alarms" as ViewKey },
    { title: "Timers", description: "Start a countdown or interval", view: "timers" as ViewKey },
    { title: "World time", description: "Compare your saved cities", view: "tools" as ViewKey },
    { title: "Settings", description: "Change appearance and preferences", view: "settings" as ViewKey },
  ].filter((item) => `${item.title} ${item.description}`.toLowerCase().includes(searchTerm.toLowerCase()));

  const nav = [
    { id: "clock" as ViewKey, label: "Clock", icon: Clock3 },
    { id: "alarms" as ViewKey, label: "Alarms", icon: AlarmClock },
    { id: "timers" as ViewKey, label: "Timers", icon: TimerReset },
    { id: "tools" as ViewKey, label: "Tools", icon: Globe2 },
    { id: "settings" as ViewKey, label: "Settings", icon: Settings2 },
  ];

  const renderClock = () => (
    <div className="clock-page">
      <section className="clock-canvas" aria-label="Current time">
        <div className="clock-main">
          <div>
            <div className="clock-meta"><span className="signal-line" /><p>Local time · {localZone.split("/").join(" · ")}</p></div>
            <div className={`time-display ${settings.clockStyle === "Compact" ? "compact" : ""} ${settings.clockStyle === "Editorial" ? "editorial" : ""}`}>
              {currentTime}{settings.showSeconds && <span className="time-seconds">:{seconds}</span>}
            </div>
            <div className="date-row"><span>{dateLabel}</span><span>{settings.hour24 ? "24 hour" : "12 hour"} time</span></div>
          </div>
          <div className="clock-footer">
            <div className="layout-switcher" aria-label="Clock style">
              {clockStyles.map((style) => <button key={style} type="button" className={`layout-chip ${settings.clockStyle === style ? "is-selected" : ""}`} onClick={() => setSettings((current) => ({ ...current, clockStyle: style }))}>{style}</button>)}
            </div>
            <div className="local-zone">{zoneLabel(localZone)}<br />{settings.showSeconds ? "seconds visible" : "seconds hidden"}</div>
          </div>
        </div>
        <aside className="clock-side">
          <div className="analog-wrap"><AnalogClock date={now} /><p className="analog-caption">Precision, not noise</p></div>
          <div className="next-alarm">
            <p className="eyebrow">Next alarm</p>
            {nextAlarm ? <div className="next-time">{nextAlarm.alarm.time}<span>{nextAlarm.alarm.label} · {timeUntilLabel(nextAlarm.at)}</span></div> : <p className="empty-next">Nothing scheduled. Rest is unscripted.</p>}
          </div>
        </aside>
      </section>
      <section className="status-strip" aria-label="Current clock status">
        <div className="status-item"><div className="status-label">Saved layouts</div><div className="status-value">{clockStyles.length} styles</div></div>
        <div className="status-item"><div className="status-label">Active timers</div><div className="status-value">{activeTimers || "None"}</div></div>
        <div className="status-item"><div className="status-label">World cities</div><div className="status-value">{worldCities.length} saved</div></div>
      </section>
    </div>
  );

  const renderAlarms = () => (
    <div className="section-page">
      <SectionHeader kicker="Wake" title="Alarms, considered." subtitle="Build a schedule that stays quiet until it needs to be heard. Every alarm is saved in this browser." />
      <div className="section-grid">
        <section className="panel">
          <div className="panel-head"><div><p className="panel-kicker">Your schedule</p><h2 className="panel-title">{alarms.length ? `${alarms.length} saved alarm${alarms.length === 1 ? "" : "s"}` : "No alarms yet"}</h2></div><button type="button" className="quiet-button" onClick={() => { setAlarmTime("07:00"); document.getElementById("alarm-time")?.focus(); }}><Plus className="button-icon" /> Add</button></div>
          <div className="panel-body">
            {alarms.length === 0 ? <div className="empty-search">Create your first alarm below. It will persist locally, even after this tab closes.</div> : alarms.map((alarm) => <div className={`alarm-row ${alarm.enabled ? "" : "is-disabled"}`} key={alarm.id}>
              <div className="alarm-time">{alarm.time}</div><div className="alarm-copy"><div className="alarm-label">{alarm.label}</div><div className="alarm-repeat">{alarm.repeat.length ? alarm.repeat.join(" · ") : "One time"}</div></div>
              <Toggle on={alarm.enabled} label={`Turn ${alarm.label} ${alarm.enabled ? "off" : "on"}`} onClick={() => toggleAlarm(alarm.id)} />
              <button type="button" className="row-menu" aria-label={`Remove ${alarm.label}`} onClick={() => removeAlarm(alarm.id)}><Trash2 size={16} /></button>
            </div>)}
          </div>
          <div className="add-alarm">
            <div className="form-grid"><input id="alarm-time" className="input" type="time" value={alarmTime} onChange={(event) => setAlarmTime(event.target.value)} aria-label="Alarm time" /><input className="input" value={alarmLabel} onChange={(event) => setAlarmLabel(event.target.value)} placeholder="Label, e.g. Morning run" aria-label="Alarm label" /><button type="button" className="primary-button" onClick={addAlarm}><Plus className="button-icon" /> Save alarm</button></div>
            <div className="weekday-picker" aria-label="Repeat days">{days.map((day) => <button type="button" key={day} className={`day-button ${alarmRepeat.includes(day) ? "is-on" : ""}`} onClick={() => setAlarmRepeat((current) => current.includes(day) ? current.filter((item) => item !== day) : [...current, day])}>{day.slice(0, 1)}</button>)}</div>
          </div>
        </section>
        <aside className="panel">
          <div className="panel-head"><div><p className="panel-kicker">At a glance</p><h2 className="panel-title">Alarm status</h2></div><AlarmClock size={18} /></div>
          <div className="side-list">
            <div className="compact-stat"><span className="compact-stat-label">Next</span><span className="compact-stat-value">{nextAlarm ? nextAlarm.alarm.time : "—"}</span></div>
            <div className="compact-stat"><span className="compact-stat-label">Enabled</span><span className="compact-stat-value">{alarms.filter((alarm) => alarm.enabled).length}</span></div>
            <div className="compact-stat"><span className="compact-stat-label">Repeat schedule</span><span className="compact-stat-value">{alarms.some((alarm) => alarm.repeat.length) ? "Active" : "None"}</span></div>
          </div>
          <div className="panel-body"><p className="section-subtitle">For reliable wake-up delivery outside a browser, use the accompanying native Android project. This companion preserves all schedule data locally.</p></div>
        </aside>
      </div>
    </div>
  );

  const renderTimers = () => (
    <div className="section-page">
      <SectionHeader kicker="Measure" title="Intervals with intent." subtitle="Run multiple timers, then use the stopwatch for the moments that cannot be predicted." />
      <div className="section-grid">
        <section className="panel">
          <div className="panel-head"><div><p className="panel-kicker">Countdowns</p><h2 className="panel-title">{timers.length ? `${timers.length} timer${timers.length === 1 ? "" : "s"}` : "Start a timer"}</h2></div><TimerReset size={18} /></div>
          <div className="panel-body">
            {timers.length === 0 ? <div className="empty-search">No countdowns running. Quick-start a preset from the right, or create a precise custom interval below.</div> : timers.map((timer) => {
              const progress = timer.duration ? Math.max(0, Math.min(1, 1 - timer.remaining / timer.duration)) : 0;
              return <div className="timer-row" key={timer.id}>
                <svg className="timer-ring" viewBox="0 0 46 46" style={{ "--progress": progress } as React.CSSProperties} aria-label={`${Math.round(progress * 100)} percent complete`}><circle className="track" cx="23" cy="23" r="18" /><circle className="progress" cx="23" cy="23" r="18" /></svg>
                <div><div className="timer-name">{timer.name}</div><div className="timer-meta">{timer.running ? "Running" : timer.remaining === 0 ? "Complete" : "Paused"}</div><div className="timer-remaining">{formatDuration(timer.remaining)}</div></div>
                <div className="timer-controls"><button type="button" className="mini-action" aria-label={timer.running ? "Pause timer" : "Start timer"} onClick={() => toggleTimer(timer.id)}>{timer.running ? <Pause /> : <Play />}</button><button type="button" className="mini-action" aria-label="Reset timer" onClick={() => resetTimer(timer.id)}><RotateCcw /></button><button type="button" className="mini-action" aria-label="Remove timer" onClick={() => removeTimer(timer.id)}><X /></button></div>
              </div>;
            })}
          </div>
          <div className="add-alarm"><div className="form-grid"><input className="input" type="number" min="1" value={timerMinutes} onChange={(event) => setTimerMinutes(event.target.value)} aria-label="Timer duration in minutes" placeholder="Minutes" /><input className="input" value={timerName} onChange={(event) => setTimerName(event.target.value)} aria-label="Timer name" placeholder="Timer name" /><button type="button" className="primary-button" onClick={() => addTimer(Number(timerMinutes) * 60_000, timerName || "Custom timer")}><Play className="button-icon" /> Start</button></div></div>
        </section>
        <aside className="tool-stack">
          <section className="panel"><div className="panel-head"><div><p className="panel-kicker">Quick start</p><h2 className="panel-title">Presets</h2></div><Zap size={17} /></div><div className="preset-grid">{[1, 5, 10, 15, 25, 30, 45, 60].map((minutes) => <button type="button" key={minutes} className="preset" onClick={() => addTimer(minutes * 60_000, `${minutes} min timer`)}><strong>{minutes} min</strong><span>Start now</span></button>)}</div></section>
          <section className="panel"><div className="panel-head"><div><p className="panel-kicker">Elapsed</p><h2 className="panel-title">Stopwatch</h2></div><Clock3 size={18} /></div><div className="stopwatch-display">{formatDuration(currentStopwatch, true)}</div><div className="stopwatch-actions"><button type="button" className="primary-button" onClick={startStopwatch}>{stopwatch.running ? <Pause className="button-icon" /> : <Play className="button-icon" />}{stopwatch.running ? "Pause" : "Start"}</button><button type="button" className="outline-button" disabled={!stopwatch.running} onClick={lapStopwatch}>Lap</button><button type="button" className="icon-button" aria-label="Reset stopwatch" onClick={resetStopwatch}><RotateCcw className="button-icon" /></button></div>{stopwatch.laps.length > 0 && <div className="lap-list">{stopwatch.laps.map((lap, index) => <div className="lap-row" key={`${lap}-${index}`}><span>Lap {stopwatch.laps.length - index}</span><span>{formatDuration(lap, true)}</span></div>)}</div>}</section>
        </aside>
      </div>
    </div>
  );

  const renderTools = () => {
    const conversionDate = new Date();
    const [hours, minutes] = conversionTime.split(":").map(Number);
    conversionDate.setHours(hours, minutes, 0, 0);
    const conversionResult = timeForZone(conversionDate, conversionTo, settings.hour24);
    return <div className="section-page">
      <SectionHeader kicker="Coordinate" title="Time beyond here." subtitle="World time, focused sessions, and useful date tools live together without becoming a drawer full of clutter." />
      <div className="tool-stack">
        <div className="section-grid">
          <section className="panel"><div className="world-visual"><p className="eyebrow">World clock</p><h3>Make distance legible.</h3></div><div className="panel-body">{worldCities.map((city) => <div className="world-row" key={city.id}><div><div className="world-name">{city.name}</div><div className="world-zone">{zoneLabel(city.zone)} · {dateForZone(now, city.zone)}</div></div><div className="world-time">{timeForZone(now, city.zone, settings.hour24)}</div><button type="button" className={`day-dot ${isNight(now, city.zone) ? "night" : ""}`} aria-label={`Remove ${city.name}`} onClick={() => removeWorldCity(city.id)} /></div>)}</div><div className="add-alarm"><div className="form-grid"><select className="select" value={cityToAdd} onChange={(event) => setCityToAdd(event.target.value)} aria-label="City to add">{cityCatalog.map((city) => <option key={city.id} value={city.id}>{city.name}</option>)}</select><div /><button type="button" className="primary-button" onClick={addWorldCity}><Plus className="button-icon" /> Add city</button></div></div></section>
          <aside className="tool-stack"><section className="tool-hero"><p className="eyebrow">Focus timer</p><h3>One span. No noise.</h3><p>A compact session timer with a visible finish line. Start a period and return to the work.</p><button type="button" className="primary-button" onClick={() => startFocus(25)}><Play className="button-icon" /> Begin 25 minutes</button></section><section className="panel"><div className="panel-head"><div><p className="panel-kicker">Focus options</p><h2 className="panel-title">Choose your span</h2></div></div><div className="preset-grid">{[25, 50, 90, 5].map((minutes) => <button type="button" className="preset" key={minutes} onClick={() => startFocus(minutes)}><strong>{minutes} min</strong><span>{minutes === 5 ? "Short break" : "Focus session"}</span></button>)}</div></section></aside>
        </div>
        <div className="section-grid">
          <section className="panel"><div className="panel-head"><div><p className="panel-kicker">Date context</p><h2 className="panel-title">Today, precisely</h2></div><CalendarDays size={18} /></div><div className="date-stat-grid"><div className="date-stat"><div className="status-label">Week number</div><strong>{Math.ceil((((now.getTime() - new Date(now.getFullYear(), 0, 1).getTime()) / 86_400_000) + new Date(now.getFullYear(), 0, 1).getDay() + 1) / 7)}</strong></div><div className="date-stat"><div className="status-label">Day of year</div><strong>{dayOfYear}</strong></div><div className="date-stat"><div className="status-label">Days remaining</div><strong>{daysRemaining}</strong></div></div></section>
          <section className="panel"><div className="panel-head"><div><p className="panel-kicker">Timezone converter</p><h2 className="panel-title">A direct translation</h2></div><Globe2 size={18} /></div><div className="converter"><div><label className="status-label" htmlFor="conversion-time">Local time</label><input id="conversion-time" className="input" type="time" value={conversionTime} onChange={(event) => setConversionTime(event.target.value)} /></div><ArrowRight className="converter-arrow" /><div><label className="status-label" htmlFor="conversion-zone">Destination</label><select id="conversion-zone" className="select" value={conversionTo} onChange={(event) => setConversionTo(event.target.value)}>{cityCatalog.map((city) => <option key={city.id} value={city.zone}>{city.name}</option>)}</select></div></div><div className="converter-result">{conversionTime} in {cityCatalog.find((city) => city.zone === conversionFrom)?.name ?? "local time"}<strong>{conversionResult} in {cityCatalog.find((city) => city.zone === conversionTo)?.name}</strong></div></section>
        </div>
      </div>
    </div>;
  };

  const renderSettings = () => (
    <div className="section-page">
      <SectionHeader kicker="Arrange" title="Made to stay out of the way." subtitle="Preferences remain local to this browser. Change the instrument, not your attention." />
      <div className="section-grid">
        <section className="panel"><div className="panel-head"><div><p className="panel-kicker">Appearance</p><h2 className="panel-title">Your visual instrument</h2></div><Settings2 size={18} /></div><div className="settings-list">
          <div className="setting-row"><div><div className="setting-label">Theme</div><div className="setting-description">Choose a paper field, deep ink, or your system setting.</div></div><div className="segment">{(["light", "dark", "system"] as ThemeMode[]).map((theme) => <button type="button" key={theme} className={settings.theme === theme ? "is-on" : ""} onClick={() => setSettings((current) => ({ ...current, theme }))}>{theme}</button>)}</div></div>
          <div className="setting-row"><div><div className="setting-label">Signal color</div><div className="setting-description">One accent is enough to clarify a live state.</div></div><div className="signal-options">{["#D6472D", "#3E6D8B", "#466A55"].map((color) => <button type="button" key={color} className={`accent-dot ${settings.accent === color ? "is-on" : ""}`} onClick={() => setSettings((current) => ({ ...current, accent: color }))} aria-label={`Use ${color} accent`}><span style={{ background: color }} /></button>)}</div></div>
          <div className="setting-row"><div><div className="setting-label">Seconds</div><div className="setting-description">Show a live seconds readout on the primary clock.</div></div><Toggle on={settings.showSeconds} label="Toggle seconds" onClick={() => setSettings((current) => ({ ...current, showSeconds: !current.showSeconds }))} /></div>
          <div className="setting-row"><div><div className="setting-label">Time format</div><div className="setting-description">Use a twelve- or twenty-four-hour instrument.</div></div><div className="segment"><button type="button" className={!settings.hour24 ? "is-on" : ""} onClick={() => setSettings((current) => ({ ...current, hour24: false }))}>12 h</button><button type="button" className={settings.hour24 ? "is-on" : ""} onClick={() => setSettings((current) => ({ ...current, hour24: true }))}>24 h</button></div></div>
        </div></section>
        <aside className="tool-stack">
          <section className="panel"><div className="panel-head"><div><p className="panel-kicker">Your time</p><h2 className="panel-title">Local insights</h2></div><ChevronDown size={18} /></div><div className="panel-body"><div className="insight-number">{analytics.timersCompleted + analytics.alarmsCreated}</div><p className="section-subtitle">meaningful time actions saved on this device. Nothing is sent away.</p></div><div className="side-list"><div className="compact-stat"><span className="compact-stat-label">Alarms created</span><span className="compact-stat-value">{analytics.alarmsCreated}</span></div><div className="compact-stat"><span className="compact-stat-label">Timers completed</span><span className="compact-stat-value">{analytics.timersCompleted}</span></div><div className="compact-stat"><span className="compact-stat-label">Focus sessions</span><span className="compact-stat-value">{analytics.focusSessions}</span></div></div><div className="panel-body"><button type="button" className="text-button" onClick={() => { setAnalytics(defaultAnalytics); setHistory([]); notify("Local insights cleared."); }}><Trash2 className="button-icon" /> Clear analytics</button></div></section>
          <section className="panel"><div className="panel-head"><div><p className="panel-kicker">Local history</p><h2 className="panel-title">Recent events</h2></div></div><div className="panel-body">{history.length ? history.slice(0, 5).map((entry) => <div className="history-row" key={entry.id}><span className="history-mark" /><span className="history-copy">{entry.label}</span><span className="history-date">{new Intl.DateTimeFormat([], { hour: "2-digit", minute: "2-digit" }).format(entry.at)}</span></div>) : <div className="empty-search">No local events yet. Start a timer or save an alarm and your activity will appear here.</div>}</div></section>
        </aside>
      </div>
    </div>
  );

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><img src="/assets/clock-signal-mark.png" alt="Premium Clock mark" className="brand-mark" /><span className="brand-name">PREMIUM:TIME</span></div>
      <nav className="nav-stack" aria-label="Primary navigation">{nav.map((item) => { const Icon = item.icon; return <button type="button" key={item.id} className={`nav-item ${view === item.id ? "is-active" : ""}`} onClick={() => switchView(item.id)}><Icon />{item.label}</button>; })}</nav>
      <div className="sidebar-foot"><button type="button" className="nav-item" onClick={() => setSearchOpen(true)}><Search />Search</button><div className="foot-row"><span>Quick find</span><span className="foot-shortcut">⌘ K</span></div></div>
    </aside>
    <main className="workspace">
      <header className="topbar"><div><p className="eyebrow">Premium Clock</p><div className="topbar-title">{nav.find((item) => item.id === view)?.label}</div></div><div className="topbar-actions"><a className="outline-button apk-download" href="https://github.com/spdedsec/premium-clock/releases/download/v1.0.0/premium-clock-android-v1.0.0-debug.apk" download><Download className="button-icon" /> Download APK</a><button type="button" className="outline-button" onClick={() => setSearchOpen(true)}><Search className="button-icon" /> Search <span className="foot-shortcut">⌘ K</span></button><button type="button" className="icon-button" aria-label="Toggle dark mode" onClick={() => setSettings((current) => ({ ...current, theme: current.theme === "dark" ? "light" : "dark" }))}>{settings.theme === "dark" ? <Sun className="button-icon" /> : <Moon className="button-icon" />}</button></div></header>
      {view === "clock" && renderClock()}{view === "alarms" && renderAlarms()}{view === "timers" && renderTimers()}{view === "tools" && renderTools()}{view === "settings" && renderSettings()}
      <footer className="site-credit">Velvex Labs <span>·</span> spdedsec</footer>
    </main>
    <nav className="mobile-nav" aria-label="Mobile navigation">{nav.map((item) => { const Icon = item.icon; return <button type="button" key={item.id} className={view === item.id ? "is-active" : ""} onClick={() => switchView(item.id)}><Icon />{item.label}</button>; })}</nav>
    {notice && <div className="notice"><Check size={15} />{notice}</div>}
    {searchOpen && <div className="search-scrim" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setSearchOpen(false); }}><div className="search-dialog" role="dialog" aria-modal="true" aria-label="Search the clock app"><div className="search-input-wrap"><Search /><input autoFocus className="search-input" value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} placeholder="Find an alarm, timer, tool, or setting" /><button type="button" className="icon-button" onClick={() => setSearchOpen(false)} aria-label="Close search"><X className="button-icon" /></button></div><div className="search-results">{searchActions.length ? searchActions.map((action) => <button type="button" key={action.title} className="search-result" onClick={() => { switchView(action.view); setSearchOpen(false); setSearchTerm(""); }}><div><strong>{action.title}</strong><span>{action.description}</span></div><ArrowRight /></button>) : <div className="empty-search">Nothing matches “{searchTerm}”.</div>}</div></div></div>}
  </div>;
}

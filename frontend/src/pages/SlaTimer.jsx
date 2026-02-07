import { useState, useEffect, memo } from 'react';

const SlaTimer = memo(({ deadline }) => {
    const [currentTime, setCurrentTime] = useState(Date.now());
    
    useEffect(() => {
        const interval = setInterval(() => {
            setCurrentTime(Date.now());
        }, 1000);
        
        return () => clearInterval(interval);
    }, []);
    
    if (!deadline) return "—";
    
    const diff = new Date(deadline).getTime() - currentTime;
    if (diff <= 0) return "SLA BREACHED";
    
    const m = Math.floor(diff / 60000);
    const s = Math.floor((diff % 60000) / 1000);
    
    if (m > 60) {
        const h = Math.floor(m / 60);
        const remainingM = m % 60;
        return `${h}h ${remainingM}m`;
    }
    
    return `${m}m ${s}s`;
});

export default SlaTimer;
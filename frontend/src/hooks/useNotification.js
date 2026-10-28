import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';

export const useNotifications = (userRole) => {
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [showNotificationPanel, setShowNotificationPanel] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [toast, setToast] = useState(null);

    const dismissToast = useCallback(() => {
        setToast(null);
    }, []);

    // Auto-dismiss toast after 5 seconds
    useEffect(() => {
        if (toast) {
            const timer = setTimeout(() => {
                setToast(null);
            }, 5000);
            return () => clearTimeout(timer);
        }
    }, [toast]);

    const fetchNotifications = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            
            const token = localStorage.getItem('token');
            if (!token) {
                throw new Error('No authentication token found');
            }

            let endpoint = '';
            let params = {};
            
            switch(userRole) {
                case 'ADMIN':
                    endpoint = '/notifications/admin';
                    const department = localStorage.getItem("department");
                    if (department) {
                        params = { department };
                    } else {
                        throw new Error('Department not found for admin');
                    }
                    break;
                case 'PROFESSOR':
                    endpoint = '/notifications/professor';
                    break;
                case 'STAFF':
                    endpoint = '/notifications/staff';
                    break;
                default:
                    throw new Error(`Invalid user role: ${userRole}`);
            }

            const response = await api.get(endpoint, {
                params: params,
                timeout: 8000
            });

            // Handle the response format correctly
            let notificationsData = [];
            let unreadCountData = 0;

            if (response.data && response.data.notifications) {
                // New format: { notifications: [...], unreadCount: X }
                notificationsData = response.data.notifications;
                unreadCountData = response.data.unreadCount || 0;
            } else if (Array.isArray(response.data)) {
                // Fallback: direct array response
                notificationsData = response.data;
                unreadCountData = response.data.filter(n => !n.isRead).length;
            } else {
                // Empty response
                notificationsData = [];
                unreadCountData = 0;
            }

            // Format notifications
            const formattedNotifications = notificationsData.map(notification => ({
                id: notification.id,
                message: notification.message || notification.msg,
                type: notification.type || 'info',
                eventType: notification.eventType,
                entityId: notification.entityId,
                isRead: notification.isRead || false,
                createdAt: notification.createdAt || new Date().toISOString(),
                department: notification.department
            })).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

            setNotifications(formattedNotifications);
            setUnreadCount(unreadCountData);

        } catch (err) {
            console.error('Failed to fetch notifications:', err.message);
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, [userRole]);

    const markAsRead = async (notificationId) => {
        try {
            await api.put(`/notifications/${notificationId}/read`);

            // Update local state
            setNotifications(prev =>
                prev.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
            );
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch (err) {
            console.error('Failed to mark as read:', err);
        }
    };

    const markAllAsRead = async () => {
        try {
            await api.put('/notifications/mark-all-read');

            // Update all notifications as read
            setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
            setUnreadCount(0);
        } catch (err) {
            console.error('Failed to mark all as read:', err);
        }
    };

    const deleteNotification = async (notificationId) => {
        try {
            await api.delete(`/notifications/${notificationId}`);

            // Remove from state
            setNotifications(prev => prev.filter(n => n.id !== notificationId));
            
            // Update count if it was unread
            const wasUnread = notifications.find(n => n.id === notificationId)?.isRead === false;
            if (wasUnread) {
                setUnreadCount(prev => Math.max(0, prev - 1));
            }
        } catch (err) {
            console.error('Failed to delete notification:', err);
        }
    };

    // 1. Initial fetch and slow fallback poll every 60 seconds
    useEffect(() => {
        fetchNotifications();
        
        // Poll every 60 seconds for updates
        const intervalId = setInterval(fetchNotifications, 60000);
        
        return () => clearInterval(intervalId);
    }, [fetchNotifications]);

    // 2. Real-time push via SSE with backoff reconnection
    useEffect(() => {
        let isMounted = true;
        let eventSource = null;
        let reconnectTimeout = null;
        let retryDelay = 1000; // Exponential backoff starts at 1s, capped at 30s

        const connectSSE = async () => {
            const token = localStorage.getItem('token');
            if (!token || !isMounted) return;

            try {
                // Request a stream ticket
                const ticketRes = await api.post('/notifications/stream-ticket');
                const ticket = ticketRes.data?.ticket;
                if (!ticket || !isMounted) return;

                const baseURL = api.defaults.baseURL || '/api';
                const streamUrl = `${baseURL}/notifications/stream?ticket=${encodeURIComponent(ticket)}`;

                eventSource = new EventSource(streamUrl);

                eventSource.onopen = () => {
                    retryDelay = 1000; // Reset backoff on successful open
                };

                const handleIncomingNotification = (event) => {
                    if (!isMounted) return;
                    try {
                        const rawData = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                        if (!rawData || !rawData.id) return;

                        const newNotification = {
                            id: rawData.id,
                            message: rawData.message || rawData.msg,
                            type: rawData.type || 'info',
                            eventType: rawData.eventType,
                            entityId: rawData.entityId,
                            isRead: rawData.isRead || false,
                            createdAt: rawData.createdAt || new Date().toISOString(),
                            department: rawData.department
                        };

                        setNotifications(prev => {
                            if (prev.some(n => n.id === newNotification.id)) {
                                return prev;
                            }
                            return [newNotification, ...prev];
                        });

                        if (!newNotification.isRead) {
                            setUnreadCount(prev => prev + 1);
                        }

                        // Show a small toast when a new notification arrives
                        setToast({
                            id: newNotification.id,
                            message: newNotification.message,
                            type: newNotification.type || 'info',
                            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                        });
                    } catch (parseErr) {
                        console.error('Failed to parse SSE notification payload:', parseErr);
                    }
                };

                eventSource.addEventListener('notification', handleIncomingNotification);
                eventSource.onmessage = handleIncomingNotification;

                eventSource.onerror = (err) => {
                    if (!isMounted) return;
                    console.warn('SSE connection closed/error, fetching latest list and reconnecting...', err);
                    if (eventSource) {
                        eventSource.close();
                        eventSource = null;
                    }

                    // On error, fetch the latest list once
                    fetchNotifications();

                    // Reconnect with backoff
                    reconnectTimeout = setTimeout(() => {
                        if (isMounted) {
                            retryDelay = Math.min(retryDelay * 2, 30000);
                            connectSSE();
                        }
                    }, retryDelay);
                };

            } catch (err) {
                if (!isMounted) return;
                console.warn('Failed to obtain SSE ticket, fetching latest list and retrying...', err.message);
                
                fetchNotifications();

                reconnectTimeout = setTimeout(() => {
                    if (isMounted) {
                        retryDelay = Math.min(retryDelay * 2, 30000);
                        connectSSE();
                    }
                }, retryDelay);
            }
        };

        connectSSE();

        return () => {
            isMounted = false;
            if (eventSource) {
                eventSource.close();
            }
            if (reconnectTimeout) {
                clearTimeout(reconnectTimeout);
            }
        };
    }, [userRole, fetchNotifications]);

    return {
        notifications,
        unreadNotifications: notifications.filter(n => !n.isRead),
        unreadCount,
        loading,
        error,
        showNotificationPanel,
        setShowNotificationPanel,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        refreshNotifications: fetchNotifications,
        toast,
        dismissToast
    };
};

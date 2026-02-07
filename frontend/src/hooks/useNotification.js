import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';

export const useNotifications = (userRole) => {
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [showNotificationPanel, setShowNotificationPanel] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

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
                    endpoint = '/api/notifications/admin';
                    const department = localStorage.getItem("department");
                    if (department) {
                        params = { department };
                    } else {
                        throw new Error('Department not found for admin');
                    }
                    break;
                case 'PROFESSOR':
                    endpoint = '/api/notifications/professor';
                    break;
                case 'STAFF':
                    endpoint = '/api/notifications/staff';
                    break;
                default:
                    throw new Error(`Invalid user role: ${userRole}`);
            }

            const response = await axios.get(`http://localhost:8080${endpoint}`, {
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                params: params,
                timeout: 8000
            });

            console.log('Notifications API Response:', response.data);

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
                message: notification.message,
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
            setNotifications([]);
            setUnreadCount(0);
        } finally {
            setLoading(false);
        }
    }, [userRole]);

    const markAsRead = async (notificationId) => {
        try {
            const token = localStorage.getItem('token');
            await axios.put(
                `http://localhost:8080/api/notifications/${notificationId}/read`,
                {},
                { headers: { Authorization: `Bearer ${token}` } }
            );

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
            const token = localStorage.getItem('token');
            await axios.put(
                'http://localhost:8080/api/notifications/mark-all-read',
                {},
                { headers: { Authorization: `Bearer ${token}` } }
            );

            // Update all notifications as read
            setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
            setUnreadCount(0);
        } catch (err) {
            console.error('Failed to mark all as read:', err);
        }
    };

    const deleteNotification = async (notificationId) => {
        try {
            const token = localStorage.getItem('token');
            await axios.delete(
                `http://localhost:8080/api/notifications/${notificationId}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );

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

    useEffect(() => {
        // Initial fetch
        fetchNotifications();
        
        // Poll every 30 seconds for updates
        const intervalId = setInterval(fetchNotifications, 30000);
        
        return () => clearInterval(intervalId);
    }, [fetchNotifications]);

    return {
        notifications,
        unreadCount,
        loading,
        error,
        showNotificationPanel,
        setShowNotificationPanel,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        refreshNotifications: fetchNotifications
    };
};
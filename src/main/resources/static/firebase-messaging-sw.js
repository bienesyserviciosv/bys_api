// Firebase Messaging Service Worker
importScripts('https://www.gstatic.com/firebasejs/9.22.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.22.0/firebase-messaging-compat.js');

// Your Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyAO58DciUD4yfT4KijeugE-lCD01NCTAhk",
    authDomain: "bienes-y-servicios-fc46f.firebaseapp.com",
    projectId: "bienes-y-servicios-fc46f",
    storageBucket: "bienes-y-servicios-fc46f.firebasestorage.app",
    messagingSenderId: "862650667877",
    appId: "1:862650667877:web:94d78c782f9936de39c78c"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Initialize Firebase Messaging
const messaging = firebase.messaging();

// Handle background messages
messaging.onBackgroundMessage((payload) => {
    console.log('Received background message:', payload);

    const notificationTitle = payload.notification?.title || 'Notification';
    const notificationOptions = {
        body: payload.notification?.body || '',
        icon: '/favicon.ico', // You can add an icon
        data: payload.data || {}
    };

    return self.registration.showNotification(notificationTitle, notificationOptions);
});

// Handle notification clicks
self.addEventListener('notificationclick', (event) => {
    console.log('Notification clicked:', event);

    event.notification.close();

    // You can add custom click handling here
    // For example, focus on a specific window or navigate to a page
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then((clients) => {
                // If a window is already open, focus it
                for (let client of clients) {
                    if (client.url.includes('/fcm-test')) {
                        return client.focus();
                    }
                }
                // Otherwise, open the FCM test page
                return clients.openWindow('/api/v1/fcm-test');
            })
    );
});




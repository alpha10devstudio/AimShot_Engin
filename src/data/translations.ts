export type Language = 'en' | 'bn' | 'hi';

export interface Translations {
  // App brand & slogans
  appTitle: string;
  appEngine: string;
  appSlogan: string;
  toolkitBadge: string;
  featuredTag: string;
  proToolkitTag: string;

  // Bottom Navigation
  navHome: string;
  navPremium: string;
  navSettings: string;

  // Home Screen
  supportedGames: string;
  viewAll: string;
  youAreNotLoggedIn: string;
  loginDesc: string;
  loginNow: string;
  launchTheGame: string;
  launchingGame: string;
  gameActive: string;
  gameLocked: string;
  selectedGame: string;
  activeStatus: string;
  lockedStatus: string;
  tapToSelect: string;

  // Carousel
  carouselTitle1: string;
  carouselSubtitle1: string;
  carouselTag1: string;
  carouselTitle2: string;
  carouselSubtitle2: string;
  carouselTag2: string;
  carouselTitle3: string;
  carouselSubtitle3: string;
  carouselTag3: string;

  // Premium Screen
  premiumStatus: string;
  statusActive: string;
  statusFree: string;
  statusExpired: string;
  expiresIn: string;
  days: string;
  notActivated: string;
  featuresUnlocked: string;
  featuresLocked: string;
  getActivationKey: string;
  getFreeKey: string;
  getPaidKey: string;
  membershipDetails: string;
  planType: string;
  startDate: string;
  expiryDate: string;
  daysRemaining: string;
  upgradeRenew: string;

  // Settings Screen
  settingsTitle: string;
  appTheme: string;
  darkMode: string;
  lightMode: string;
  language: string;
  selectedLanguageName: string;
  notifications: string;
  notificationsOn: string;
  notificationsOff: string;
  hapticFeedback: string;
  deviceInformation: string;
  privacySecurity: string;
  helpSupport: string;
  aboutApp: string;
  version: string;
  logOut: string;
  confirmLogout: string;
  logoutDesc: string;
  cancel: string;

  // Language Dialog
  selectLanguageTitle: string;
  selectLanguageDesc: string;
  englishLang: string;
  bengaliLang: string;
  hindiLang: string;
  apply: string;
  close: string;

  // Device Info
  deviceSpecsTitle: string;
  deviceId: string;
  androidVersion: string;
  deviceModel: string;
  systemRam: string;
  resolution: string;
  processor: string;

  // Seller Contact
  sellerTitle: string;
  officialDistributor: string;
  whatsappSupport: string;
  telegramChannel: string;
  emailSupport: string;
  trustedSeller: string;

  // Launch dialog & toasts
  gameLaunchedSuccess: string;
  gameLaunchWarning: string;
  keyVerified: string;
  sessionCleared: string;
}

export const TRANSLATIONS: Record<Language, Translations> = {
  en: {
    appTitle: 'AimShot',
    appEngine: 'Engine',
    appSlogan: 'Sharper Aim • Better Game • Higher You',
    toolkitBadge: 'Pro Gaming Toolkit',
    featuredTag: 'FEATURED V1.0',
    proToolkitTag: 'PRO TOOLKIT',

    navHome: 'Home',
    navPremium: 'Premium',
    navSettings: 'Settings',

    supportedGames: 'Supported Games',
    viewAll: 'View All',
    youAreNotLoggedIn: 'You are not logged in',
    loginDesc: 'Login to access premium features and full tools.',
    loginNow: 'Login Now',
    launchTheGame: 'Launch The Game',
    launchingGame: 'Launching Game with Aim Engine...',
    gameActive: 'Aim Assist Active',
    gameLocked: 'Locked • Pro Key Required',
    selectedGame: 'Selected Game',
    activeStatus: 'ACTIVE',
    lockedStatus: 'LOCKED',
    tapToSelect: 'Tap to Select',

    carouselTitle1: 'Auto Aim & Guideline Engine',
    carouselSubtitle1: 'Engineered for Carrom Disc Pool & Billiards with millimeter precision',
    carouselTag1: 'TACTICAL AIM v1.0',
    carouselTitle2: 'Zero Recoil & FPS Stabilization',
    carouselSubtitle2: 'Ultra low latency 120 FPS game acceleration & crosshair assist',
    carouselTag2: 'FPS BOOST 120Hz',
    carouselTitle3: 'Advanced Anti-Ban & Shield Security',
    carouselSubtitle3: 'Hardware-level isolated touch emulation and bypass algorithms',
    carouselTag3: 'SECURE CORE',

    premiumStatus: 'Premium Status',
    statusActive: 'Active',
    statusFree: 'Free',
    statusExpired: 'Expired',
    expiresIn: 'Expires In',
    days: 'Days',
    notActivated: 'Not Activated',
    featuresUnlocked: 'Premium Features Unlocked',
    featuresLocked: 'Premium Features Locked',
    getActivationKey: 'Get Activation Key',
    getFreeKey: 'Get Free Key',
    getPaidKey: 'Get Paid Key',
    membershipDetails: 'Membership Details',
    planType: 'Plan Type',
    startDate: 'Start Date',
    expiryDate: 'Expiry Date',
    daysRemaining: 'Days Remaining',
    upgradeRenew: 'Upgrade / Renew',

    settingsTitle: 'Settings',
    appTheme: 'App Theme',
    darkMode: 'Dark Mode',
    lightMode: 'Light Mode',
    language: 'Language',
    selectedLanguageName: 'English (US)',
    notifications: 'Notifications',
    notificationsOn: 'On',
    notificationsOff: 'Off',
    hapticFeedback: 'Haptic Feedback',
    deviceInformation: 'Device Information',
    privacySecurity: 'Privacy & Security',
    helpSupport: 'Help & Support',
    aboutApp: 'About App',
    version: 'Version',
    logOut: 'Log Out',
    confirmLogout: 'Confirm Logout',
    logoutDesc: 'Are you sure you want to log out? Your active session will be cleared and premium license detached.',
    cancel: 'Cancel',

    selectLanguageTitle: 'Select Language',
    selectLanguageDesc: 'Choose your preferred language for AimShot Engine.',
    englishLang: 'English (English)',
    bengaliLang: 'বাংলা (Bengali)',
    hindiLang: 'हिन्दी (Hindi)',
    apply: 'Apply Language',
    close: 'Close',

    deviceSpecsTitle: 'Device Specifications',
    deviceId: 'Device ID',
    androidVersion: 'Android OS Version',
    deviceModel: 'Model & Manufacturer',
    systemRam: 'Total System RAM',
    resolution: 'Display Resolution',
    processor: 'CPU / Security Core',

    sellerTitle: 'Official Seller Contact',
    officialDistributor: 'AimShot Official Store',
    whatsappSupport: 'WhatsApp Support',
    telegramChannel: 'Telegram Channel',
    emailSupport: 'Email Support',
    trustedSeller: '100% Trusted & Verified Official Seller',

    gameLaunchedSuccess: 'AimShot Engine launched for',
    gameLaunchWarning: 'Game not installed locally. Launching in AimShot Simulation Mode.',
    keyVerified: 'Key Verified! Premium active: 18 Days remaining.',
    sessionCleared: 'Session cleared. Logged out.'
  },

  bn: {
    appTitle: 'এইমশট',
    appEngine: 'ইঞ্জিন',
    appSlogan: 'শার্পার এইম • বেটার গেম • হায়ার ইউ',
    toolkitBadge: 'প্রো গেমিং টুলকিট',
    featuredTag: 'ফিচার্ড V১.০',
    proToolkitTag: 'প্রো টুলকিট',

    navHome: 'হোম',
    navPremium: 'প্রিমিয়াম',
    navSettings: 'সেটিংস',

    supportedGames: 'সমর্থিত গেমসমূহ',
    viewAll: 'সব দেখুন',
    youAreNotLoggedIn: 'আপনি লগইন করেননি',
    loginDesc: 'প্রিমিয়াম ফিচার এবং সম্পূর্ণ টুলস পেতে লগইন করুন।',
    loginNow: 'লগইন করুন',
    launchTheGame: 'লঞ্চ দ্য গেম',
    launchingGame: 'এইম ইঞ্জিনের সাথে গেম চালু হচ্ছে...',
    gameActive: 'এইম অ্যাসিস্ট সক্রিয়',
    gameLocked: 'লক করা • প্রো কি প্রয়োজন',
    selectedGame: 'নির্বাচিত গেম',
    activeStatus: 'সক্রিয়',
    lockedStatus: 'লকড',
    tapToSelect: 'নির্বাচন করতে চাপুন',

    carouselTitle1: 'অটো এইম ও গাইডলাইন ইঞ্জিন',
    carouselSubtitle1: 'ক্যারম ডিস্ক পুল এবং বিলিয়ার্ডসের জন্য নির্ভুল নির্দেশিকা',
    carouselTag1: 'ট্যাকটিকাল এইম v১.০',
    carouselTitle2: 'জিরো রিকয়েল ও এফপিএস স্ট্যাবিলাইজার',
    carouselSubtitle2: 'আল্ট্রা লো ল্যাটেন্সি ১২০ এফপিএস গেম এক্সিলারেশন',
    carouselTag2: 'এফপিএস বুস্ট ১২০ হার্টজ',
    carouselTitle3: 'অ্যাডভান্সড অ্যান্টি-ব্যান ও শিল্ড সিকিউরিটি',
    carouselSubtitle3: 'হার্ডওয়্যার-লেভেল আইসোলেটেড টাচ এমুলেশন ও সুরক্ষা অ্যালগরিদম',
    carouselTag3: 'সিকিউর কোর',

    premiumStatus: 'প্রিমিয়াম স্ট্যাটাস',
    statusActive: 'সক্রিয়',
    statusFree: 'ফ্রি',
    statusExpired: 'মেয়াদোত্তীর্ণ',
    expiresIn: 'মেয়াদ শেষ হবে',
    days: 'দিন',
    notActivated: 'সক্রিয় নয়',
    featuresUnlocked: 'প্রিমিয়াম ফিচার আনলক হয়েছে',
    featuresLocked: 'প্রিমিয়াম ফিচার লক করা',
    getActivationKey: 'অ্যাক্টিভেশন কি সংগ্রহ করুন',
    getFreeKey: 'ফ্রি কি নিন',
    getPaidKey: 'পেইড কি কিনুন',
    membershipDetails: 'মেম্বারশিপের বিবরণ',
    planType: 'প্ল্যানের ধরন',
    startDate: 'শুরুর তারিখ',
    expiryDate: 'মেয়াদ শেষের তারিখ',
    daysRemaining: 'অবশিষ্ট দিন',
    upgradeRenew: 'আপগ্রেড / রিনিউ করুন',

    settingsTitle: 'সেটিংস',
    appTheme: 'অ্যাপ থিম',
    darkMode: 'ডার্ক মোড',
    lightMode: 'লাইট মোড',
    language: 'ভাষা (Language)',
    selectedLanguageName: 'বাংলা (Bengali)',
    notifications: 'বিজ্ঞপ্তি',
    notificationsOn: 'চালু',
    notificationsOff: 'বন্ধ',
    hapticFeedback: 'হ্যাপটিক ভাইব্রেশন',
    deviceInformation: 'ডিভাইস তথ্য',
    privacySecurity: 'প্রাইভেসি ও সিকিউরিটি',
    helpSupport: 'সহায়তা ও সাপোর্ট',
    aboutApp: 'অ্যাপ সম্পর্কে',
    version: 'সংস্করণ',
    logOut: 'লগ আউট',
    confirmLogout: 'লগ আউট নিশ্চিতকরণ',
    logoutDesc: 'আপনি কি নিশ্চিতভাবে লগ আউট করতে চান? আপনার বর্তমান সেশন বন্ধ হবে এবং প্রিমিয়াম লাইসেন্স বিচ্ছিন্ন হবে।',
    cancel: 'বাতিল',

    selectLanguageTitle: 'ভাষা নির্বাচন করুন',
    selectLanguageDesc: 'এইমশট ইঞ্জিনের জন্য আপনার পছন্দের ভাষা বেছে নিন।',
    englishLang: 'English (ইংরেজি)',
    bengaliLang: 'বাংলা (Bengali)',
    hindiLang: 'हिन्दी (হিন্দি)',
    apply: 'ভাষা প্রয়োগ করুন',
    close: 'বন্ধ করুন',

    deviceSpecsTitle: 'ডিভাইসের বিবরণ',
    deviceId: 'ডিভাইস আইডি',
    androidVersion: 'অ্যান্ড্রয়েড ওএস সংস্করণ',
    deviceModel: 'মডেল ও প্রস্তুতকারক',
    systemRam: 'মোট র‍্যাম (RAM)',
    resolution: 'ডিসপ্লে রেজোলিউশন',
    processor: 'সিপিইউ / সিকিউরিটি কোর',

    sellerTitle: 'অফিসিয়াল সেলারের সাথে যোগাযোগ',
    officialDistributor: 'এইমশট অফিসিয়াল স্টোর',
    whatsappSupport: 'হোয়াটসঅ্যাপ সাপোর্ট',
    telegramChannel: 'টেলিগ্রাম চ্যানেল',
    emailSupport: 'ইমেইল সাপোর্ট',
    trustedSeller: '১০০% বিশ্বস্ত ও ভেরিফাইড অফিসিয়াল সেলার',

    gameLaunchedSuccess: 'এইমশট ইঞ্জিন চালু হয়েছে:',
    gameLaunchWarning: 'গেমটি ফোনে ইনস্টল নেই। সিমুলেশন মোডে চালু হচ্ছে।',
    keyVerified: 'কি ভেরিফাইড! প্রিমিয়াম সক্রিয়: ১৮ দিন বাকি।',
    sessionCleared: 'সেশন পরিষ্কার করা হয়েছে। লগ আউট সম্পন্ন।'
  },

  hi: {
    appTitle: 'एमशॉट',
    appEngine: 'इंजन',
    appSlogan: 'शार्पर एम • बेहतर गेम • उच्च रैंक',
    toolkitBadge: 'प्रो गेमिंग टूलकिट',
    featuredTag: 'फीचर्ड V1.0',
    proToolkitTag: 'प्रो टूलकिट',

    navHome: 'होम',
    navPremium: 'प्रीमियम',
    navSettings: 'सेटिंग्स',

    supportedGames: 'समर्थित गेम्स',
    viewAll: 'सभी देखें',
    youAreNotLoggedIn: 'आप लॉग इन नहीं हैं',
    loginDesc: 'प्रीमियम सुविधाओं और संपूर्ण टूल्स के लिए लॉग इन करें।',
    loginNow: 'लॉग इन करें',
    launchTheGame: 'लॉन्च द गेम',
    launchingGame: 'एम इंजन के साथ गेम शुरू हो रहा है...',
    gameActive: 'एम असिस्ट सक्रिय',
    gameLocked: 'लॉक है • प्रो की आवश्यक',
    selectedGame: 'चयनित गेम',
    activeStatus: 'सक्रिय',
    lockedStatus: 'लॉक्ड',
    tapToSelect: 'चुनने के लिए टैप करें',

    carouselTitle1: 'ऑटो एम एवं गाइडलाइन इंजन',
    carouselSubtitle1: 'कैरम डिस्क पूल और बिलियर्ड्स के लिए सटीक निशाना गाइडलाइन',
    carouselTag1: 'टैक्टिकल एम v1.0',
    carouselTitle2: 'जीरो रिकॉइल और एफपीएस स्टेबलाइजर',
    carouselSubtitle2: 'अल्ट्रा लो लेटेंसी 120 एफपीएस गेम एक्सेलेरेशन',
    carouselTag2: 'एफपीएस बूस्ट 120Hz',
    carouselTitle3: 'उन्नत एंटी-बैन एवं शील्ड सुरक्षा',
    carouselSubtitle3: 'हार्डवेयर-स्तर का आइसोलेटेड टच इम्यूलेशन और सुरक्षा कोड',
    carouselTag3: 'सुरक्षित कोर',

    premiumStatus: 'प्रीमियम स्थिति',
    statusActive: 'सक्रिय',
    statusFree: 'निःशुल्क',
    statusExpired: 'समाप्त',
    expiresIn: 'वैधता शेष',
    days: 'दिन',
    notActivated: 'सक्रिय नहीं',
    featuresUnlocked: 'प्रीमियम सुविधाएं अनलॉक हैं',
    featuresLocked: 'प्रीमियम सुविधाएं लॉक हैं',
    getActivationKey: 'एक्टिवेशन की प्राप्त करें',
    getFreeKey: 'फ्री की लें',
    getPaidKey: 'सशुल्क की खरीदें',
    membershipDetails: 'सदस्यता विवरण',
    planType: 'प्लान प्रकार',
    startDate: 'प्रारंभ तिथि',
    expiryDate: 'समाप्ति तिथि',
    daysRemaining: 'शेष दिन',
    upgradeRenew: 'अपग्रेड / नवीनीकरण',

    settingsTitle: 'सेटिंग्स',
    appTheme: 'ऐप थीम',
    darkMode: 'डार्क मोड',
    lightMode: 'लाइट मोड',
    language: 'भाषा (Language)',
    selectedLanguageName: 'हिन्दी (Hindi)',
    notifications: 'सूचनाएं',
    notificationsOn: 'चालू',
    notificationsOff: 'बंद',
    hapticFeedback: 'हैप्टिक फीडबैक',
    deviceInformation: 'डिवाइस जानकारी',
    privacySecurity: 'गोपनीयता एवं सुरक्षा',
    helpSupport: 'मदद एवं सहायता',
    aboutApp: 'ऐप के बारे में',
    version: 'संस्करण',
    logOut: 'लॉग आउट',
    confirmLogout: 'लॉग आउट की पुष्टि',
    logoutDesc: 'क्या आप निश्चित रूप से लॉग आउट करना चाहते हैं? आपका सक्रिय सत्र समाप्त हो जाएगा।',
    cancel: 'रद्द करें',

    selectLanguageTitle: 'भाषा चुनें',
    selectLanguageDesc: 'एमशॉट इंजन के लिए अपनी पसंदीदा भाषा चुनें।',
    englishLang: 'English (अंग्रेज़ी)',
    bengaliLang: 'বাংলা (बंगाली)',
    hindiLang: 'हिन्दी (Hindi)',
    apply: 'भाषा लागू करें',
    close: 'बंद करें',

    deviceSpecsTitle: 'डिवाइस विनिर्देश',
    deviceId: 'डिवाइस आईडी',
    androidVersion: 'एंड्रॉइड ओएस संस्करण',
    deviceModel: 'मॉडल एवं निर्माता',
    systemRam: 'कुल रैम (RAM)',
    resolution: 'डिस्प्ले रेजोल्यूशन',
    processor: 'सीपीयू / सुरक्षा कोर',

    sellerTitle: 'आधिकारिक विक्रेता संपर्क',
    officialDistributor: 'एमशॉट आधिकारिक स्टोर',
    whatsappSupport: 'व्हाट्सएप सहायता',
    telegramChannel: 'टेलीग्राम चैनल',
    emailSupport: 'ईमेल सहायता',
    trustedSeller: '100% विश्वसनीय एवं सत्यापित आधिकारिक विक्रेता',

    gameLaunchedSuccess: 'एमशॉट इंजन लॉन्च किया गया:',
    gameLaunchWarning: 'गेम फ़ोन में स्थापित नहीं है। सिमुलेशन मोड में शुरू हो रहा है।',
    keyVerified: 'की सत्यापित! प्रीमियम सक्रिय: 18 दिन शेष।',
    sessionCleared: 'सत्र साफ़ किया गया। लॉग आउट पूर्ण।'
  }
};

<template>
  <div class="min-h-screen flex flex-col">
    <!-- Navbar -->
    <nav class="fixed top-0 left-0 right-0 z-50 bg-white/80 dark:bg-gray-900/80 backdrop-blur-lg border-b border-gray-100 dark:border-gray-800">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- Logo -->
          <router-link to="/" class="flex items-center gap-2">
            <img
              src="/assets/brand/logo.png"
              alt="CargoHub"
              class="w-9 h-9 object-contain"
            />
            <span class="text-xl font-bold text-gray-900 dark:text-white">Cargo<span class="text-primary">Hub</span></span>
          </router-link>

          <!-- Desktop nav links -->
          <div class="hidden md:flex items-center gap-8">
            <a href="#servicios" class="text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-primary transition-colors">{{ $t('landing.navbar.services') }}</a>
            <a href="#precios" class="text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-primary transition-colors">{{ $t('landing.navbar.pricing') }}</a>
            <a href="#como-funciona" class="text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-primary transition-colors">{{ $t('landing.navbar.howItWorks') }}</a>
          </div>

          <!-- Auth buttons + Language -->
          <div class="flex items-center gap-3">
            <ThemeToggle />
            <LanguageSwitcher />
            <router-link
              to="/login"
              class="text-sm font-medium text-gray-700 dark:text-gray-300 hover:text-primary transition-colors"
            >
              {{ $t('landing.navbar.login') }}
            </router-link>
            <router-link
              to="/register"
              class="px-4 py-2 bg-primary text-white text-sm font-semibold rounded-lg hover:bg-primary-700 transition-colors shadow-sm"
            >
              {{ $t('landing.navbar.register') }}
            </router-link>
          </div>
        </div>
      </div>
    </nav>

    <!-- Page content -->
    <main class="flex-1 pt-16">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="bg-gray-900 text-gray-400">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
          <!-- Brand -->
          <div class="md:col-span-2">
            <div class="flex items-center gap-2 mb-4">
              <img
                src="/assets/brand/logo.png"
                alt="CargoHub"
                class="w-8 h-8 object-contain"
              />
              <span class="text-lg font-bold text-white">CargoHub</span>
            </div>
            <p class="text-sm leading-relaxed max-w-md">
              {{ $t('landing.footer.description') }}
            </p>
          </div>

          <!-- Links -->
          <div>
            <h4 class="text-sm font-semibold text-white mb-3">{{ $t('landing.footer.platform') }}</h4>
            <ul class="space-y-2 text-sm">
              <li><a href="#servicios" class="hover:text-white transition-colors">{{ $t('landing.footer.services') }}</a></li>
              <li><a href="#precios" class="hover:text-white transition-colors">{{ $t('landing.footer.pricing') }}</a></li>
              <li><a href="#como-funciona" class="hover:text-white transition-colors">{{ $t('landing.footer.howItWorks') }}</a></li>
            </ul>
          </div>

          <!-- Legal -->
          <div>
            <h4 class="text-sm font-semibold text-white mb-3">{{ $t('landing.footer.legal') }}</h4>
            <ul class="space-y-2 text-sm">
              <li><a href="#" class="hover:text-white transition-colors">{{ $t('landing.footer.terms') }}</a></li>
              <li><a href="#" class="hover:text-white transition-colors">{{ $t('landing.footer.privacy') }}</a></li>
              <li><a href="#" class="hover:text-white transition-colors">{{ $t('landing.footer.contact') }}</a></li>
            </ul>
          </div>
        </div>

        <div class="border-t border-gray-800 mt-10 pt-6 text-center text-xs">
          <p>&copy; {{ new Date().getFullYear() }} {{ $t('landing.footer.copyright') }}</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
</script>

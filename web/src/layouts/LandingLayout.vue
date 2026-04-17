<template>
  <div class="min-h-screen flex flex-col">
    <!-- Navbar -->
    <nav class="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-lg border-b border-gray-100">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- Logo -->
          <router-link to="/" class="flex items-center gap-2">
            <div class="w-9 h-9 bg-gradient-to-br from-primary-500 to-primary-700 rounded-xl flex items-center justify-center">
              <i class="pi pi-truck text-white text-lg"></i>
            </div>
            <span class="text-xl font-bold text-gray-900">Cargo<span class="text-primary">Hub</span></span>
          </router-link>

          <!-- Desktop nav links -->
          <div class="hidden md:flex items-center gap-8">
            <a href="#servicios" class="text-sm font-medium text-gray-600 hover:text-primary transition-colors">Servicios</a>
            <a href="#precios" class="text-sm font-medium text-gray-600 hover:text-primary transition-colors">Precios</a>
            <a href="#como-funciona" class="text-sm font-medium text-gray-600 hover:text-primary transition-colors">Cómo Funciona</a>
          </div>

          <!-- Auth buttons -->
          <div class="flex items-center gap-3">
            <router-link
              to="/login"
              class="text-sm font-medium text-gray-700 hover:text-primary transition-colors"
            >
              Iniciar Sesión
            </router-link>
            <router-link
              to="/register"
              class="px-4 py-2 bg-primary text-white text-sm font-semibold rounded-lg hover:bg-primary-700 transition-colors shadow-sm"
            >
              Registrarse
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
              <div class="w-8 h-8 bg-gradient-to-br from-primary-400 to-primary-600 rounded-lg flex items-center justify-center">
                <i class="pi pi-truck text-white"></i>
              </div>
              <span class="text-lg font-bold text-white">CargoHub</span>
            </div>
            <p class="text-sm leading-relaxed max-w-md">
              Plataforma líder en gestión de transporte y logística. Conectamos empresas con conductores
              profesionales para un servicio rápido, seguro y eficiente.
            </p>
          </div>

          <!-- Links -->
          <div>
            <h4 class="text-sm font-semibold text-white mb-3">Plataforma</h4>
            <ul class="space-y-2 text-sm">
              <li><a href="#servicios" class="hover:text-white transition-colors">Servicios</a></li>
              <li><a href="#precios" class="hover:text-white transition-colors">Precios</a></li>
              <li><a href="#como-funciona" class="hover:text-white transition-colors">Cómo Funciona</a></li>
            </ul>
          </div>

          <!-- Legal -->
          <div>
            <h4 class="text-sm font-semibold text-white mb-3">Legal</h4>
            <ul class="space-y-2 text-sm">
              <li><a href="#" class="hover:text-white transition-colors">Términos de Servicio</a></li>
              <li><a href="#" class="hover:text-white transition-colors">Política de Privacidad</a></li>
              <li><a href="#" class="hover:text-white transition-colors">Contacto</a></li>
            </ul>
          </div>
        </div>

        <div class="border-t border-gray-800 mt-10 pt-6 text-center text-xs">
          <p>&copy; {{ new Date().getFullYear() }} CargoHub. Todos los derechos reservados.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

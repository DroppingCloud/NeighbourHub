import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Application, FamilyBinding, GovService, ServiceBooking, WorkOrder } from '@/types'

export const useServiceStore = defineStore('service', () => {
  const govServices = ref<GovService[]>([])
  const applications = ref<Application[]>([])
  const workOrders = ref<WorkOrder[]>([])
  const bookings = ref<ServiceBooking[]>([])
  const familyBindings = ref<FamilyBinding[]>([])

  function addApplication(application: Application) {
    applications.value.unshift(application)
  }

  function updateApplicationStatus(id: string, status: Application['status'], remark?: string) {
    const app = applications.value.find(item => item.id === id)
    if (app) {
      app.status = status
      app.lastUpdate = new Date().toLocaleString()
      if (remark) app.remark = remark
    }
  }

  function addBooking(booking: ServiceBooking) {
    bookings.value.unshift(booking)
  }

  function updateBookingStatus(id: string, status: ServiceBooking['status']) {
    const booking = bookings.value.find(item => item.id === id)
    if (booking) booking.status = status
  }

  function addFeedback(bookingId: string, feedback: string, rating: number) {
    const booking = bookings.value.find(item => item.id === bookingId)
    if (booking) {
      booking.feedback = feedback
      booking.rating = rating
    }
  }

  return {
    govServices,
    applications,
    workOrders,
    bookings,
    familyBindings,
    addApplication,
    updateApplicationStatus,
    addBooking,
    updateBookingStatus,
    addFeedback
  }
})

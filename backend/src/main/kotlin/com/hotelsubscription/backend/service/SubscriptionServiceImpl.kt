package com.hotelsubscription.backend.service

import com.hotelsubscription.backend.dto.SubscriptionResponse
import com.hotelsubscription.backend.entity.Status
import com.hotelsubscription.backend.entity.Subscription
import com.hotelsubscription.backend.entity.Term
import com.hotelsubscription.backend.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SubscriptionServiceImpl(
    private val subscriptionRepository: SubscriptionRepository
) : SubscriptionService {

    override fun startSubscription(hotelId: Long, startDate: LocalDate, term: Term): SubscriptionResponse {
        val activeSubscriptions = subscriptionRepository.findByHotelIdAndStatus(hotelId, Status.ACTIVE)
        if (activeSubscriptions.isEmpty()) {
            val nextPaymentDate = if (term == Term.MONTHLY) startDate.plusMonths(1) else startDate.plusYears(1)
            val subscription = Subscription(
                hotelId = hotelId,
                startDate = startDate,
                nextPayment = nextPaymentDate,
                term = term,
                status = Status.ACTIVE
            )
            return subscriptionRepository.save(subscription).toResponse()
        }
        throw IllegalStateException("Hotel already has an active subscription")
    }

    override fun cancelSubscription(subscriptionId: Long): SubscriptionResponse {
        val subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow { NoSuchElementException("Subscription not found") }
        if (subscription.status == Status.ACTIVE) {
            val updatedSubscription = subscription.copy(status = Status.CANCELED, endDate = LocalDate.now())
            return subscriptionRepository.save(updatedSubscription).toResponse()
        }
        throw IllegalStateException("Cannot cancel a subscription that is not active")
    }

    override fun getAllSubscriptions(): List<SubscriptionResponse> {
        return subscriptionRepository.findAll().map { it.toResponse() }
    }

    override fun restartSubscription(subscriptionId: Long): SubscriptionResponse {
        val subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow { NoSuchElementException("Subscription not found") }
        if (subscription.status == Status.CANCELED || subscription.status == Status.EXPIRED) {
            val nextPaymentDate =
                if (subscription.term == Term.MONTHLY) LocalDate.now().plusMonths(1) else LocalDate.now().plusYears(1)
            val updatedSubscription =
                subscription.copy(status = Status.ACTIVE, nextPayment = nextPaymentDate, endDate = null)
            return subscriptionRepository.save(updatedSubscription).toResponse()
        }
        throw IllegalStateException("Subscription must be either canceled or expired to be restarted")
    }

    private fun Subscription.toResponse(): SubscriptionResponse {
        return SubscriptionResponse(
            id = this.id,
            hotelId = this.hotelId,
            startDate = this.startDate,
            nextPayment = this.nextPayment,
            endDate = this.endDate,
            term = this.term,
            status = this.status
        )
    }
}
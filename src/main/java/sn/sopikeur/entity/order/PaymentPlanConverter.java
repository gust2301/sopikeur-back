package sn.sopikeur.entity.order;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PaymentPlanConverter implements AttributeConverter<PaymentPlan, String> {

    @Override
    public String convertToDatabaseColumn(PaymentPlan attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public PaymentPlan convertToEntityAttribute(String dbData) {
        return PaymentPlan.fromValue(dbData);
    }
}

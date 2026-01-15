package sn.sopikeur.mapper;

import org.mapstruct.Mapper;
import sn.sopikeur.dto.response.admin.ContactMessageResponse;
import sn.sopikeur.dto.response.admin.PreorderRequestResponse;
import sn.sopikeur.dto.response.admin.QuoteRequestResponse;
import sn.sopikeur.entity.leads.ContactMessage;
import sn.sopikeur.entity.leads.PreorderRequest;
import sn.sopikeur.entity.leads.QuoteRequest;

@Mapper(componentModel = "spring")
public interface LeadMapper {
    QuoteRequestResponse toResponse(QuoteRequest quoteRequest);

    ContactMessageResponse toResponse(ContactMessage contactMessage);

    PreorderRequestResponse toResponse(PreorderRequest preorderRequest);
}

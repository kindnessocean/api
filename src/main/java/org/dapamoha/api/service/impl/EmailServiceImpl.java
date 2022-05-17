package org.dapamoha.api.service.impl;

import java.util.Date;
import java.util.UUID;
import org.dapamoha.api.security.domain.EmailUserDetails;
import org.dapamoha.api.service.interf.EmailService;
import org.dapamoha.api.service.mq.producer.EmailRequestProducer;
import org.dapamoha.shared.domain.exception.NotFoundObjectAppException;
import org.dapamoha.shared.domain.util.CodeUtil;
import org.dapamoha.shared.domain.util.UuidUtil;
import org.dapamoha.shared.kafka.model.emailRequest.EmailRequestKey;
import org.dapamoha.shared.kafka.model.emailRequest.EmailRequestValue;
import org.dapamoha.shared.posgresql.entity.Email;
import org.dapamoha.shared.posgresql.entity.EmailCode;
import org.dapamoha.shared.posgresql.repository.email.EmailRepository;
import org.dapamoha.shared.posgresql.repository.emailCode.EmailCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailServiceImpl implements EmailService {

    private final EmailRepository emailRepository;
    private final EmailCodeRepository emailCodeRepository;
    private final EmailRequestProducer emailRequestProducer;

    private final UuidUtil uuidUtil;

    private final CodeUtil codeUtil;

    @Value("${org.dapamoha.api.code.expiredTime}")
    private Integer expiredTime;

    public EmailServiceImpl(
            final EmailRepository emailRepository,
            final EmailCodeRepository emailCodeRepository, final EmailRequestProducer emailRequestProducer,
            final UuidUtil uuidUtil, final CodeUtil codeUtil) {
        this.emailRepository = emailRepository;
        this.emailCodeRepository = emailCodeRepository;
        this.emailRequestProducer = emailRequestProducer;
        this.uuidUtil = uuidUtil;
        this.codeUtil = codeUtil;
    }

    @Override
    public Email getEmailByAddress(final String address) {
        Email email = emailRepository.getEmailByAddress(address);

        if (email == null){
            throw new NotFoundObjectAppException("Not found Email with such address=" + address);
        }

        return email;
    }

    @Override
    public EmailCode sendEmailCode(Email email) {
        UUID uuid = uuidUtil.randomUUID();
        Date created = new Date();
        Date expiredAt = new Date(created.getTime() + expiredTime);
        Integer code = codeUtil.generateCodeAsNumber();

        EmailCode emailCode;

        if (email.getCode() != null) {
            // Update previous code
            emailCode = email.getCode();

            emailCode.setCode(code);
            emailCode.setExpiredAt(expiredAt);

            emailCode = emailCodeRepository.update(emailCode);


        } else {
            // Create another
            emailCode = EmailCode.builder()
                    .email(email)
                    .expiredAt(expiredAt)
                    .code(code)
                    .build();

            emailCode = emailCodeRepository.save(emailCode);

            email.setCode(emailCode);

            email = emailRepository.update(email);
        }

        // Push to MQ topic
        emailRequestProducer.produceRequest(
                new EmailRequestKey(uuid),
                new EmailRequestValue(
                        email.getAddress(),
                        code
                )
        );

        return emailCode;
    }

    @Override
    public EmailCode getEmailCodeByAddress(final String address) {
        Email email = emailRepository.getEmailByAddress(address);

        if (email == null || email.getCode() == null) {
            throw new NotFoundObjectAppException("Email not found for this address " + address);
        }

        return email.getCode();
    }

    @Override
    public void expireEmailCode(final EmailCode emailCode) {
        //        fixme
        emailCode.setExpiredAt(new Date());

        emailCodeRepository.update(emailCode);
    }

    @Override
    public Email getEmailByEmailUserDetails(final EmailUserDetails emailUserDetails) {
        return getEmailByAddress(emailUserDetails.getEmail());
    }

    @Override
    public Email update(final Email email) {
        return emailRepository.update(email);
    }

    @Override
    public Email create(final String address) {
        Email email = emailRepository.create(
                Email.builder().address(address).build()
        );

        EmailCode emailCode = emailCodeRepository.save(
                EmailCode.builder()
                        .email(email)
                        .build()
        );

        email.setCode(emailCode);

        return emailRepository.update(email);
    }

    @Override
    public Boolean isExistEmailByAddress(final String address) {
        return emailRepository.getEmailByAddress(address) != null;
    }
}
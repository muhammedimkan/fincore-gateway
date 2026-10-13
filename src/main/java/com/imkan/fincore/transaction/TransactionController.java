package com.imkan.fincore.transaction;

import com.imkan.fincore.transaction.dto.TransactionResponse;
import com.imkan.fincore.transaction.dto.TransferRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransactionController {

    private final TransferService transferService;

    @PostMapping("/api/transfers")
    public ResponseEntity<TransactionResponse> transfer(@RequestHeader("X-User-Id") Long userid, @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.transfer(userid, request));
    }
}

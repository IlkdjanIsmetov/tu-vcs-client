package com.ksig.vcs_cli.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCRView {
    private String tittle;
    private long baseRevisionNUmber;
    private String description;
}
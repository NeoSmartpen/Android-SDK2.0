package kr.neolab.sdk.metadata;

import kr.neolab.sdk.metadata.structure.Symbol;

public interface IMetadataListener
{
    void onSymbolDetected( Symbol[] symbols );
}
